/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.epics.gpclient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.epics.gpclient.datasource.DataSource;

/**
 * Allows to configure the type of read/write PV to create.
 *
 * @param <R> the read payload
 * @param <W> the write payload
 * @author carcassi
 */
public class PVConfiguration<R, W> implements PVReaderConfiguration<R> {

    Executor notificationExecutor;
    DataSource dataSource;
    Duration connectionTimeout;
    String connectionTimeoutMessage;
    Duration maxRate;
    List<Object> listeners;

    /**
     * Defines which DataSource should be used to read the data.
     *
     * @param dataSource a connection manager
     * @return this
     */
    public PVConfiguration<R, W> from(DataSource dataSource) {
        // TODO: check all parameters for double setting
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource can't be null");
        }
        this.dataSource = dataSource;
        return this;
    }

    /**
     * Defines on which thread the PVManager should notify the client.
     *
     * @param onThread the thread on which to notify
     * @return this
     */
    public PVConfiguration<R, W> notifyOn(Executor onThread) {
        if (this.notificationExecutor == null) {
            this.notificationExecutor = onThread;
        } else {
            throw new IllegalStateException("Already set what thread to notify");
        }
        return this;
    }
    
    public PVConfiguration<R, W> maxRate(Duration maxRate) {
        if (this.maxRate != null)
            throw new IllegalStateException("Max ragte already set");
        if (maxRate.getSeconds() == 0 && maxRate.toMillis() < 5) {
            throw new IllegalArgumentException("Current implementation limits the rate to >5ms or <200Hz (requested " + maxRate + "s)");
        }
        this.maxRate = maxRate;
        return this;
    }
    
    public PVConfiguration<R, W> connectionTimeout(Duration timeout) {
        if (this.connectionTimeout != null)
            throw new IllegalStateException("Timeout already set");
        this.connectionTimeout = timeout;
        return this;
    }
    
    public PVConfiguration<R, W> connectionTimeout(Duration timeout, String timeoutMessage) {
        connectionTimeout(timeout);
        this.connectionTimeoutMessage = timeoutMessage;
        return this;
    }

    void checkParameters() {
        // Get defaults
        if (dataSource == null) {
            dataSource = null; // TODO set defaults PVManager.getDefaultDataSource();
        }
        if (notificationExecutor == null) {
            notificationExecutor = null; // TODO set defaults PVManager.getDefaultNotificationExecutor();
        }

        // Check that a data source has been specified
        if (dataSource == null) {
            throw new IllegalStateException("You need to specify a source either "
                    + "using PVManager.setDefaultDataSource or by using "
                    + "read(...).from(dataSource).");
        }

        // Check that thread switch has been specified
        if (notificationExecutor == null) {
            throw new IllegalStateException("You need to specify a thread either "
                    + "using PVManager.setDefaultThreadSwitch or by using "
                    + "read(...).andNotify(threadSwitch).");
        }
    }
    
    public PVConfiguration<R, W>  addListener(PVReaderListener<R> listener) {
        listeners.add(listener);
        return this;
    }

    public PVConfiguration<R, W> addListener(PVWriterListener<W> listener) {
        listeners.add(listener);
        return this;
    }

    public PVConfiguration<R, W> addListener(PVListener<R, W> listener) {
        listeners.add(listener);
        return this;
    }
    
    public PV<R, W> start() {
        return null;
    }
    
}

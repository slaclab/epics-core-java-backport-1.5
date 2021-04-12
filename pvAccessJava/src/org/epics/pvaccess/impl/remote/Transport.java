/*
 * Copyright (c) 2004 by Cosylab
 *
 * The full license specifying the redistribution, modification, usage and other
 * rights and obligations is included with the distribution of this project in
 * the file "LICENSE-CAJ". If the license is not included visit Cosylab web site,
 * <http://www.cosylab.com>.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package org.epics.pvaccess.impl.remote;

import org.epics.pvaccess.plugins.SecurityPlugin.SecuritySession;
import org.epics.pvdata.pv.DeserializableControl;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.Status;

import java.net.InetSocketAddress;
import java.nio.channels.Channel;


/**
 * Interface defining transport (connection).
 *
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id$
 */
public interface Transport extends DeserializableControl, Channel {

    /**
     * Acquires transport.
     *
     * @param client client (channel) acquiring the transport
     * @return <code>true</code> if transport was granted, <code>false</code> otherwise.
     */
    boolean acquire(TransportClient client);

    /**
     * Releases transport.
     *
     * @param client client (channel) releasing the transport
     */
    void release(TransportClient client);

    /**
     * Get protocol type (tcp, udp, ssl, etc.).
     *
     * @return protocol type.
     */
    String getType();

    /**
     * Get remote address.
     *
     * @return remote address.
     */
    InetSocketAddress getRemoteAddress();

    /**
     * Get context transport is living in.
     *
     * @return context transport is living in.
     */
    Context getContext();

    /**
     * Transport protocol revision.
     *
     * @return protocol revision.
     */
    byte getRevision();

    /**
     * Get receive buffer size.
     *
     * @return receive buffer size.
     */
    int getReceiveBufferSize();

    /**
     * Get socket receive buffer size.
     *
     * @return socket receive buffer size.
     */
    int getSocketReceiveBufferSize();

    /**
     * Transport priority.
     *
     * @return protocol priority.
     */
    short getPriority();

    /**
     * Set remote transport protocol revision.
     *
     * @param revision protocol revision.
     */
    void setRemoteRevision(byte revision);

    /**
     * Set remote transport receive buffer size.
     *
     * @param receiveBufferSize receive buffer size.
     */
    void setRemoteTransportReceiveBufferSize(int receiveBufferSize);

    /**
     * Set remote transport socket receive buffer size.
     *
     * @param socketReceiveBufferSize remote socket receive buffer size.
     */
    void setRemoteTransportSocketReceiveBufferSize(int socketReceiveBufferSize);

    /**
     * Notification that transport has changed (server restarted).
     */
    void changedTransport();

    /**
     * Enqueue send request.
     *
     * @param sender sender to enqueue.
     */
    void enqueueSendRequest(TransportSender sender);

    /**
     * Waits (if needed) until transport is verified, i.e. verified() method is being called.
     *
     * @param timeoutMs timeout to wait for verification, infinite if 0.
     * @return completion status.
     */
    boolean verify(long timeoutMs);

    /**
     * Acknowledge that transport was verified.
     *
     * @param status verification status.
     */
    void verified(Status status);

    /**
     * Alive notification.
     * This method needs to be called (by newly received data or beacon)
     * at least once in this period, if not echo will be issued
     * and if there is not response to it, transport will be considered as unresponsive.
     */
    void aliveNotification();

    /**
     * Pass data to the active security plug-in session.
     *
     * @param data the data (any data), can be <code>null</code>.
     */
    void authNZMessage(PVField data);

    /**
     * Used to initialize authNZ (select security plug-in).
     *
     * @param data any data.
     */
    void authNZInitialize(Object data);

    SecuritySession getSecuritySession();

}

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

package org.epics.pvaccess.client.impl.remote.tcp;

import org.epics.pvaccess.impl.remote.*;
import org.epics.pvaccess.impl.remote.request.ResponseHandler;
import org.epics.pvaccess.util.sync.NamedLockPattern;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * Channel Access TCP connector.
 *
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id$
 */
public class BlockingTCPConnector implements Connector {

    public interface TransportFactory {
        Transport create(Context context, SocketChannel channel,
                         ResponseHandler responseHandler, int receiveBufferSize,
                         TransportClient client, short transportRevision,
                         float heartbeatInterval, short priority);
    }

    /**
     * Context instance.
     */
    private final Context context;

    /**
     * Context instance.
     */
    private final NamedLockPattern namedLocker;

    /**
     * Lock timeout.
     */
    private static final int LOCK_TIMEOUT = 20 * 1000;    // 20s

    /**
     * Number of connection tries before giving up.
     */
    private static final int CONNECT_TRIES = 3;

    /**
     * Verification timeout.
     */
    private static final int VERIFICATION_TIMEOUT = 5000;    // 5s

    /**
     * Receive buffer size.
     */
    private final int receiveBufferSize;

    /**
     * Heartbeat interval.
     */
    private final float heartbeatInterval;

    /**
     * Transport factory.
     */
    private final TransportFactory transportFactory;

    public BlockingTCPConnector(Context context, TransportFactory transportFactory, int receiveBufferSize, float heartbeatInterval) {
        this.context = context;
        this.transportFactory = transportFactory;
        this.receiveBufferSize = receiveBufferSize;
        this.heartbeatInterval = heartbeatInterval;
        namedLocker = new NamedLockPattern();
    }


    /**
     * @see org.epics.pvaccess.impl.remote.Connector#connect(org.epics.pvaccess.impl.remote.TransportClient, org.epics.pvaccess.impl.remote.request.ResponseHandler, java.net.InetSocketAddress, byte, short)
     */
    public Transport connect(TransportClient client, ResponseHandler responseHandler,
                             InetSocketAddress address, byte transportRevision, short priority)
            throws ConnectionException {

        SocketChannel socket = null;

        // first try to check cache w/o named lock...
        Transport transport = context.getTransportRegistry().get(address, priority);
        if (transport != null) {
            context.getLogger().finer("Reusing existing connection to PVA server: " + address);
            if (transport.acquire(client))
                return transport;
        }

        boolean lockAcquired = namedLocker.acquireSynchronizationObject(address, LOCK_TIMEOUT);
        if (lockAcquired) {
            try {
                // ... transport created during waiting in lock
                transport = context.getTransportRegistry().get(address, priority);
                if (transport != null) {
                    context.getLogger().finer("Reusing existing connection to PVA server: " + address);
                    if (transport.acquire(client))
                        return transport;
                }

                context.getLogger().finer("Connecting to PVA server: " + address);

                socket = tryConnect(address);

                // use blocking channel
                socket.configureBlocking(true);

                // enable TCP_NO_DELAY (disable Nagle's algorithm)
                socket.socket().setTcpNoDelay(true);

                // enable TCP_KEEPALIVE
                socket.socket().setKeepAlive(true);

                // do NOT tune socket buffer sizes, this will disable auto-tuning

                // create transport
                transport = transportFactory.create(context, socket, responseHandler, receiveBufferSize, client, transportRevision, heartbeatInterval, priority);

                // verify
                if (!transport.verify(VERIFICATION_TIMEOUT)) {
                    context.getLogger().finer("Connection to PVA client " + address + " failed to be validated, closing it.");
                    transport.close();
                    throw new ConnectionException("Failed to verify connection to '" + address + "'.", address, ProtocolType.tcp.name(), null);
                }

                // TODO send security token

                context.getLogger().finer("Connected to PVA server: " + address);

                return transport;
            } catch (Throwable th) {
                // close socket, if open
                try {
                    if (socket != null)
                        socket.close();
                } catch (Throwable t) { /* noop */ }

                throw new ConnectionException("Failed to connect to '" + address + "'.", address, ProtocolType.tcp.name(), th);
            } finally {
                namedLocker.releaseSynchronizationObject(address);
            }
        } else {
            throw new ConnectionException("Failed to obtain synchronization lock for '" + address + "', possible deadlock.", address, ProtocolType.tcp.name(), null);
        }
    }

    /**
     * Tries to connect to the given addresses.
     *
     * @param address address
     * @return socket channel
     * @throws IOException for IO exceptions
     */
    private SocketChannel tryConnect(InetSocketAddress address)
            throws IOException {

        IOException lastException = null;

        for (int tryCount = 0; tryCount < BlockingTCPConnector.CONNECT_TRIES; tryCount++) {

            // sleep for a while
            if (tryCount > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }

            context.getLogger().finest("Opening socket to PVA server " + address + ", attempt " + (tryCount + 1) + ".");

            try {
                return SocketChannel.open(address);
            } catch (IOException ioe) {
                lastException = ioe;
            }


        }

        throw lastException;
    }

}

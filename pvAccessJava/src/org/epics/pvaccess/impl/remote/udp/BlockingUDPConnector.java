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

package org.epics.pvaccess.impl.remote.udp;

import org.epics.pvaccess.impl.remote.*;
import org.epics.pvaccess.impl.remote.request.ResponseHandler;
import org.epics.util.compat.jdk5.net.MulticastSocket;

import java.io.IOException;
import java.net.InetSocketAddress;


/**
 * UDP broadcast connector.
 * This is not a real connector since it binds.
 *
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id$
 */
public class BlockingUDPConnector implements Connector {

    /**
     * Context instance.
     */
    private final Context context;

    /**
     * Send address.
     */
    private final InetSocketAddress[] sendAddresses;

    /**
     * Reuse socket flag.
     */
    private final boolean reuseSocket;

    public BlockingUDPConnector(Context context, boolean reuseSocket, InetSocketAddress[] sendAddresses) {
        this.context = context;
        this.reuseSocket = reuseSocket;
        this.sendAddresses = sendAddresses;
    }

    /**
     * NOTE: transport client is ignored for broadcast (UDP).
     *
     * @see org.epics.pvaccess.impl.remote.Connector#connect(org.epics.pvaccess.impl.remote.TransportClient, org.epics.pvaccess.impl.remote.request.ResponseHandler, java.net.InetSocketAddress, byte, short)
     */
    public Transport connect(TransportClient client, ResponseHandler responseHandler, InetSocketAddress bindAddress, byte transportRevision, short priority)
            throws ConnectionException {
        context.getLogger().finer("Creating datagram socket to " + bindAddress + ".");

        MulticastSocket socket = null;
        try {
            try {
                socket = new MulticastSocket(bindAddress);
            } catch (IOException e) {
                // If fail to bind to specified address then bind to any address.
                socket = new MulticastSocket();
                context.getLogger().warning("Failed to bind to '" + bindAddress);
            }

            if (reuseSocket)
                socket.setReuseAddress(true);

            // create transport
            return new BlockingUDPTransport(context, responseHandler, socket,
                    bindAddress, sendAddresses);
        } catch (Throwable th) {
            // close socket, if open
            try {
                if (socket != null)
                    socket.close();
            } catch (Throwable t) { /* noop */ }

            throw new ConnectionException("Failed to bind to '" + bindAddress + "'.", bindAddress, ProtocolType.udp.name(), th);
        }

    }


}

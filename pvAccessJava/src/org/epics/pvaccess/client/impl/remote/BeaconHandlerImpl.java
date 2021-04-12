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

package org.epics.pvaccess.client.impl.remote;

import org.epics.pvaccess.impl.remote.Transport;
import org.epics.pvdata.pv.PVField;

import java.net.InetSocketAddress;
import java.util.Arrays;


/**
 * PVA beacon handler.
 *
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id$
 */
public class BeaconHandlerImpl implements BeaconHandler {

    /**
     * Context instance.
     */
    private final ClientContextImpl context;

    /**
     * The protocol (transport), "tcp" for pvAccess TCP/IP.
     */
    private final String protocol;

    /**
     * Remote address.
     */
    private final InetSocketAddress responseFrom;

    /**
     * Server GUID.
     */
    private byte[] serverGUID = null;

    /**
     * Server change count.
     */
    private int serverChangeCount;

    /**
     * Constructor.
     *
     * @param context      context ot handle.
     * @param protocol     that protocol this handles.
     * @param responseFrom server to handle.
     */
    public BeaconHandlerImpl(ClientContextImpl context, String protocol, InetSocketAddress responseFrom) {
        this.context = context;
        this.protocol = protocol;
        this.responseFrom = responseFrom;
    }

    /**
     * Update beacon period and do analytical checks (server restored, routing problems, etc.)
     */
    public void beaconNotify(InetSocketAddress from, byte remoteTransportRevision,
                             long timestamp, byte[] guid, int sequentialID,
                             int changeCount, PVField data) {
        boolean networkChanged = updateBeacon(guid, changeCount);
        if (networkChanged)
            changedTransport();
    }

    /**
     * Update beacon.
     */
    private synchronized boolean updateBeacon(byte[] guid, int changeCount) {
        // first beacon notification check
        if (serverGUID == null) {
            serverGUID = guid;
            serverChangeCount = changeCount;

            // new server up...
            context.newServerDetected();

            return false;
        }

        final boolean networkChange = Arrays.equals(serverGUID, guid);
        if (networkChange) {
            // update startup time and change count
            serverGUID = guid;
            serverChangeCount = changeCount;

            context.newServerDetected();

            return true;
        } else if (serverChangeCount != changeCount) {
            //  change count
            serverChangeCount = changeCount;

            // TODO be more specific (possible optimizations)
            context.newServerDetected();

            return true;
        }

        return false;
    }

    /**
     * Changed transport (server restarted) notify.
     */
    private void changedTransport() {
        Transport[] transports = context.getTransportRegistry().get(responseFrom);
        if (transports == null)
            return;

        // notify all
        for (Transport transport : transports)
            transport.changedTransport();
    }
}

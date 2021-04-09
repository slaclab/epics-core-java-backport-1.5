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

package org.epics.pvaccess.util;

import org.epics.util.compat.jdk5.net.InterfaceAddress;
import org.epics.util.compat.jdk5.net.NetworkInterface;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

import static org.epics.pvaccess.PVAConstants.PVA_BROADCAST_PORT;

/**
 * <code>InetAddress</code> utility methods.
 *
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id$
 */
public class InetAddressUtil {

    private static final String HOSTNAME_KEY = "HOSTNAME";
    private static final String STRIP_HOSTNAME_KEY = "STRIP_HOSTNAME";

    private static final String MULTICAST_GROUP_KEY = "EPICS_PVA_MULTICAST_GROUP";
    private static final String MULTICAST_GROUP_DEFAULT = "239.219.1.200";
    public static final InetSocketAddress MULTICAST_GROUP = new InetSocketAddress(MULTICAST_GROUP_DEFAULT, PVA_BROADCAST_PORT);

    private static final Set<NetworkInterface> MULTICAST_NIFS = new HashSet<NetworkInterface>();
    private static boolean MULTICAST_NIFS_INITIALISED = false;

    private static final Set<NetworkInterface> LOOPBACK_NIFS = new HashSet<NetworkInterface>();
    private static boolean LOOPBACK_NIFS_INITIALISED = false;

    private static final Set<InetAddress> BROADCAST_LIST = new HashSet<InetAddress>(10);
    private static boolean BROADCAST_LIST_INITIALISED = false;

    private static InetSocketAddress[] BROADCAST_SOCKETS;

    private static String hostName = null;

    private static String MULTICAST_GROUP_OVERRIDE = null;

    public static synchronized String getHostName() {
        if (hostName == null)
            hostName = internalGetHostName();
        return hostName;
    }

    private static String internalGetHostName() {
        // default fallback
        String hostName = "localhost";

        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            hostName = localAddress.getHostName();
        } catch (Throwable uhe) {    // not only UnknownHostException
            // try with environment variable
            try {
                String envHN = System.getenv(HOSTNAME_KEY);
                if (envHN != null)
                    hostName = envHN;
            } catch (Throwable th) {
                // in case not supported by JVM/OS
            }

            // and system property (overrides env. var.)
            hostName = System.getProperty(HOSTNAME_KEY, hostName);
        }

        if (System.getProperties().contains(STRIP_HOSTNAME_KEY)) {
            int dotPos = hostName.indexOf('.');
            if (dotPos > 0)
                hostName = hostName.substring(0, dotPos);
        }

        return hostName;
    }

    /**
     * Get broadcast addresses.
     *
     * @param port port to be added to get socket address.
     * @return array of broadcast addresses with given port.
     */
    public static synchronized InetSocketAddress[] getBroadcastAddresses(int port) {
        if (BROADCAST_SOCKETS != null) {
            return BROADCAST_SOCKETS;
        }

        Set<InetAddress> broadcastList = getBroadcastAddresses();
        ArrayList<InetSocketAddress> list = new ArrayList<InetSocketAddress>(broadcastList.size());
        for (InetAddress broadcastAddress : broadcastList) {
            InetSocketAddress isa = new InetSocketAddress(broadcastAddress, port);
            if (!list.contains(isa))
                list.add(isa);
        }

        BROADCAST_SOCKETS = new InetSocketAddress[list.size()];
        list.toArray(BROADCAST_SOCKETS);
        return BROADCAST_SOCKETS;
    }

    /**
     * Get a set of broadcast addresses.
     *
     * @return set of broadcast addresses.
     */
    public static synchronized Set<InetAddress> getBroadcastAddresses() {
        if (BROADCAST_LIST_INITIALISED) {
            return BROADCAST_LIST;
        }

        BROADCAST_LIST_INITIALISED = true;
        Enumeration<NetworkInterface> nets;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException se) {
            // fallback
            try {
                BROADCAST_LIST.add(InetAddress.getByAddress(new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255}));
            } catch (UnknownHostException ignored) {
            }
            return BROADCAST_LIST;
        }

        while (nets.hasMoreElements()) {
            NetworkInterface net = nets.nextElement();
            try {
                if (net.isUp()) {
                    List<InterfaceAddress> interfaceAddresses = net.getInterfaceAddresses();
                    if (interfaceAddresses != null)
                        for (InterfaceAddress addr : interfaceAddresses) {
                            InetAddress ba = addr.getBroadcast();
                            if (ba != null && !addr.getAddress().getHostAddress().startsWith("169"))        // Set class takes care of duplicates
                                BROADCAST_LIST.add(ba);
                        }
                }
            } catch (Throwable th) {
                // some methods throw exceptions, some return null (and they shouldn't)
                // noop, skip that interface
            }
        }

        return BROADCAST_LIST;
    }

    /**
     * Get a multicast NIF.  This returns the first interface that is a multicast supporting interface
     *
     * @return a multicast capable NIF, <code>null</code> if not found.
     */
    public static NetworkInterface getFirstMulticastNIF() {
        Set<NetworkInterface> multicastNifs = getMulticastNIFs();
        if (multicastNifs.isEmpty()) {
            return null;
        } else {
            return multicastNifs.iterator().next();
        }
    }

    /**
     * For the purposes of EPICS we only want to use the loopback NIFs for multicast
     *
     * @return all loopback NIFs that are capable of multicast
     */
    public static synchronized Set<NetworkInterface> getMulticastNIFs() {
        if (!MULTICAST_NIFS_INITIALISED) {
            MULTICAST_NIFS.addAll(getLoopbackNIFs());
            MULTICAST_NIFS_INITIALISED = true;
        }

        return MULTICAST_NIFS;
    }

    /**
     * Get a loopback NIF.
     *
     * @return a loopback NIF, <code>null</code> if not found.
     */
    public static NetworkInterface getLoopbackNIF() {
        Set<NetworkInterface> loopbackNIFs = getLoopbackNIFs();
        if (loopbackNIFs.isEmpty()) {
            return null;
        } else {
            return loopbackNIFs.iterator().next();
        }
    }

    /**
     * Get a loopback NIF.
     *
     * @return all loopback NIFs.
     */
    public static synchronized Set<NetworkInterface> getLoopbackNIFs() {
        if (!LOOPBACK_NIFS_INITIALISED) {
            try {
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                while (nets.hasMoreElements()) {
                    NetworkInterface net = nets.nextElement();
                    try {
                        if (net.isUp() && net.isLoopback())
                            LOOPBACK_NIFS.add(net);
                    } catch (Throwable ignored) {
                    }
                }
            } catch (SocketException ignored) {
            }
            LOOPBACK_NIFS_INITIALISED = true;
        }
        return LOOPBACK_NIFS;
    }

    /**
     * Encode address as IPv6 address.
     *
     * @param buffer  byte-buffer where to put encoded data.
     * @param address address to encode.
     * @throws RuntimeException thrown if address is unsupported.
     */
    public static void encodeAsIPv6Address(ByteBuffer buffer, InetAddress address) throws RuntimeException {
        if (address instanceof Inet6Address)
            buffer.put(address.getAddress());    // always network byte order
        else if (address instanceof Inet4Address) {
            // IPv4 compatible IPv6 address
            // first 80-bit are 0
            buffer.putLong(0);
            buffer.putShort((short) 0);
            // next 16-bits are 1
            buffer.putShort((short) 0xFFFF);
            // following IPv4 address
            buffer.put(address.getAddress());    // always network byte order
        } else
            throw new RuntimeException("unsupported network addresses: " + address);
    }

    /**
     * Convert an integer into an IPv4 INET address.
     *
     * @param addr integer representation of a given address.
     * @return IPv4 INET address.
     */
    public static InetAddress intToIPv4Address(int addr) {
        byte[] a = new byte[4];

        a[0] = (byte) ((addr >> 24) & 0xFF);
        a[1] = (byte) ((addr >> 16) & 0xFF);
        a[2] = (byte) ((addr >> 8) & 0xFF);
        a[3] = (byte) ((addr & 0xFF));

        InetAddress res = null;
        try {
            res = InetAddress.getByAddress(a);
        } catch (UnknownHostException e) { /* noop */ }

        return res;
    }

    /**
     * Convert an IPv4 INET address to an integer.
     *
     * @param addr IPv4 INET address.
     * @return integer representation of a given address.
     * @throws IllegalArgumentException if the address is really an IPv6 address
     */
    public static int ipv4AddressToInt(InetAddress addr) {

        if (addr instanceof Inet6Address)
            throw new IllegalArgumentException("IPv6 address used in IPv4 context");

        byte[] a = addr.getAddress();

        return ((a[0] & 0xFF) << 24)
                | ((a[1] & 0xFF) << 16)
                | ((a[2] & 0xFF) << 8)
                | (a[3] & 0xFF);
    }


    /**
     * Parse space delimited addresses[:port] string and return array of <code>InetSocketAddress</code>.
     *
     * @param list        space delimited addresses[:port] string.
     * @param defaultPort port take if not specified.
     * @return array of <code>InetSocketAddress</code>.
     */
    public static InetSocketAddress[] getSocketAddressList(String list, int defaultPort) {
        return getSocketAddressList(list, defaultPort, null);
    }

    /**
     * Parse space delimited addresses[:port] string and return array of <code>InetSocketAddress</code>.
     *
     * @param list        space delimited addresses[:port] string.
     * @param defaultPort port take if not specified.
     * @param appendList  list to be appended.
     * @return array of <code>InetSocketAddress</code>.
     */
    public static InetSocketAddress[] getSocketAddressList(String list, int defaultPort, InetSocketAddress[] appendList) {
        ArrayList<InetSocketAddress> al = new ArrayList<InetSocketAddress>();

        // parse string
        StringTokenizer st = new StringTokenizer(list);
        while (st.hasMoreTokens()) {
            int port = defaultPort;
            String address = st.nextToken();

            // check port
            int pos = address.indexOf(':');
            if (pos >= 0) {
                try {
                    port = Integer.parseInt(address.substring(pos + 1));
                } catch (NumberFormatException nfe) { /* noop */ }

                address = address.substring(0, pos);
            }

            try {
                InetSocketAddress isa = new InetSocketAddress(address, port);

                // add parsed address if resolved
                if (!isa.isUnresolved())
                    al.add(isa);
            } catch (Throwable th) {
                // TODO
                th.printStackTrace();
            }
        }

        // copy to array
        int appendSize = (appendList == null) ? 0 : appendList.length;
        InetSocketAddress[] isar = new InetSocketAddress[al.size() + appendSize];
        al.toArray(isar);
        if (appendSize > 0)
            System.arraycopy(appendList, 0, isar, al.size(), appendSize);
        return isar;
    }

    /**
     * Returns the IPV4 multicast group to use for EPICS
     *
     * @return multicast group[
     * @throws UnknownHostException should never be thrown unless there is a mistake in the multicast address
     */
    public static synchronized InetAddress getMulticastGroup() throws UnknownHostException {
        // Initialise cache first time
        if (MULTICAST_GROUP_OVERRIDE == null) {
            MULTICAST_GROUP_OVERRIDE = System.getenv(MULTICAST_GROUP_KEY);

            // If not defined then try a property
            if (MULTICAST_GROUP_OVERRIDE == null) {
                MULTICAST_GROUP_OVERRIDE = System.getProperty(MULTICAST_GROUP_KEY, MULTICAST_GROUP_DEFAULT);
            }
        }

        return InetAddress.getByName(MULTICAST_GROUP_OVERRIDE);
    }
}

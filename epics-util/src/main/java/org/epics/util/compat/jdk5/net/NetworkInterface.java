package org.epics.util.compat.jdk5.net;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Implementation of NetworkfInterface networking class
 *
 * @author George McIntyre. 15-Feb-2021, SLAC
 */
public class NetworkInterface {
    private final java.net.NetworkInterface networkInterface;

    public java.net.NetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    public NetworkInterface(java.net.NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }

    public int getIndex() throws SocketException {
        Enumeration<java.net.NetworkInterface> enumeratedNetworkInterfaces = java.net.NetworkInterface.getNetworkInterfaces();
        for (int i = 0; enumeratedNetworkInterfaces.hasMoreElements(); i++) {
            if (enumeratedNetworkInterfaces.nextElement().equals(this.networkInterface)) {
                return i;
            }
        }
        return -1;
    }

    public NetworkInterface getParent() {
        return null;
    }

    public boolean isUp() throws SocketException {
        Enumeration<InetAddress> enumeratedInetAddresses = this.networkInterface.getInetAddresses();
        return enumeratedInetAddresses.hasMoreElements();
    }

    public List<InterfaceAddress> getInterfaceAddresses() {
        Enumeration<InetAddress> enumeratedInetAddresses = this.networkInterface.getInetAddresses();
        List<InterfaceAddress> interfaceAddresses = new ArrayList<InterfaceAddress>();

        while (enumeratedInetAddresses.hasMoreElements()) {
            InetAddress inetAddress = enumeratedInetAddresses.nextElement();
            interfaceAddresses.add(new InterfaceAddress(inetAddress));
        }
        return Collections.unmodifiableList(interfaceAddresses);
    }

    public boolean isLoopback() {
        Enumeration<InetAddress> enumeratedInetAddresses = this.networkInterface.getInetAddresses();
        while (enumeratedInetAddresses.hasMoreElements()) {
            if (enumeratedInetAddresses.nextElement().isLoopbackAddress())
                return true;
        }
        return false;
    }

    public boolean isPointToPoint() {
        // TODO Find a good way to determine whether point to point
        return false;
    }

    /**
     * Does this interface support multicast.
     *
     * @return true iff any of its addresses are site local but not wild card local,
     * link local, loopback or a multicast address
     * <p>
     * from: https://stackoverflow.com/questions/18747134/getting-cant-assign-requested-address-java-net-socketexception-using-ehcache
     */
    public boolean supportsMulticast() {
        Enumeration<InetAddress> inetAddresses = this.getInetAddresses();
        while (inetAddresses.hasMoreElements()) {
            InetAddress inetAddress = inetAddresses.nextElement();
            if (inetAddress.isSiteLocalAddress()
                    && !inetAddress.isAnyLocalAddress()
                    && !inetAddress.isLinkLocalAddress()
                    && !inetAddress.isLoopbackAddress()
                    && !inetAddress.isMulticastAddress()) {
                return true;
            }
        }
        return false;
    }

    public boolean isVirtual() {
        // TODO Find a good way to determine whether isVirtual

        return false;
    }

    public byte[] getHardwareAddress() {
        // TODO Find a good way to determine the mac code
        return null;
    }

    public int getMTU() {
        // TODO Find a good way to determine the MTU
        //  For the moment we will return 1500 for all addresses
        //  except localhost where we will return 16384

        if (isLoopback()) {
            return 4168;
        } else {
            return 1500;
        }
    }

    public Enumeration<NetworkInterface> getSubInterfaces() {
        return Collections.enumeration(new ArrayList<NetworkInterface>());
    }

    /// DELEGATES

    public String getName() {
        return networkInterface.getName();
    }

    public Enumeration<InetAddress> getInetAddresses() {
        return networkInterface.getInetAddresses();
    }

    public String getDisplayName() {
        return networkInterface.getDisplayName();
    }

    public static Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException {
        Enumeration<java.net.NetworkInterface> enumeratedNetworkInterfaces = java.net.NetworkInterface.getNetworkInterfaces();
        List<NetworkInterface> networkInterfaceList = new ArrayList<NetworkInterface>();
        while (enumeratedNetworkInterfaces.hasMoreElements()) {
            networkInterfaceList.add(new NetworkInterface(enumeratedNetworkInterfaces.nextElement()));
        }
        return Collections.enumeration(networkInterfaceList);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NetworkInterface)) return false;

        NetworkInterface that = (NetworkInterface) o;

        return networkInterface != null ? networkInterface.equals(that.networkInterface) : that.networkInterface == null;
    }

    @Override
    public int hashCode() {
        return networkInterface != null ? networkInterface.hashCode() : 0;
    }

    @Override
    public String toString() {
        return networkInterface.toString();
    }
}

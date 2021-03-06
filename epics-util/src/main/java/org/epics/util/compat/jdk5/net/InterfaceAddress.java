package org.epics.util.compat.jdk5.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static org.epics.util.compat.jdk5.net.NetClass.*;

/**
 * This class represents a Network Interface address. In short it's an
 * IP address, a subnet mask and a broadcast address when the address is
 * an IPv4 one. An IP address and a network prefix length in the case
 * of IPv6 address.
 *
 * @author George McIntyre. 15-Feb-2021, SLAC
 * @see java.net.NetworkInterface
 */
public class InterfaceAddress {
    private final InetAddress address;
    private final InetAddress broadcast;
    private final short maskLength;
    private final short networkPrefixLength;
    private final NetClass addressClass;
    private static boolean networkCustomised = false;
    private static final String EPICS_NETWORK_PROPERTY_NAME = "EPICS_NETWORKS_CIDR_LIST";

    /**
     * Special networks (IPV4 bytes) to look for and the prefix length to use if found
     * Network (specified with bytes followed by netmask : networkPrefixLength-to-use
     */
    private final static Map<byte[], String> NETWORKS = new HashMap<byte[], String>() {{
        put(new byte[]{(byte) 10}, "8:24");  // For all CLASS_A use 24
        put(new byte[]{(byte) 172, (byte) 16}, "12:24");  // For all CLASS_B use 24
        put(new byte[]{(byte) 192, (byte) 168}, "16:24");  // For all CLASS_C use 24
    }};

    public InterfaceAddress(InetAddress address) {
        // Add EPICS NETWORKS if specified in the environment
        // e.g. EPICS NETWORKS=172.16/12:24,
        synchronized (NETWORKS) {
            if (!networkCustomised) {
                String networkStringList = System.getenv(EPICS_NETWORK_PROPERTY_NAME);
                if (networkStringList != null) {
                    String[] networks = networkStringList.split(",");
                    for (String networkCidr : networks) {
                        String[] networkCidrComponents = networkCidr.split("/");
                        String networkString = networkCidrComponents[0];
                        String networkInfoString = networkCidrComponents[1];

                        String[] networkComponents = networkString.split("\\.");
                        byte[] b = new byte[networkComponents.length];
                        for (int i = 0; i < networkComponents.length; i++) {
                            b[i] = (byte) Short.parseShort(networkComponents[i]);
                        }
                        NETWORKS.put(b, networkInfoString);
                    }
                }
                networkCustomised = true;
            }
        }
        this.address = address;
        this.addressClass = getAddressClass();
        this.maskLength = getMaskLength();
        this.networkPrefixLength = getNetworkPrefixLength();
        this.broadcast = getBroadcastAddress();
    }

    /**
     * Returns an <code>InetAddress</code> for this address.
     *
     * @return the <code>InetAddress</code> for this address.
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Returns an <code>InetAddress</code> for the broadcast address
     * for this InterfaceAddress.
     * <p>
     * Only IPv4 networks have broadcast address therefore, in the case
     * of an IPv6 network, <code>null</code> will be returned.
     *
     * @return the <code>InetAddress</code> representing the broadcast
     * address or <code>null</code> if there is no broadcast address.
     */
    public InetAddress getBroadcast() {
        return broadcast;
    }

    /**
     * Compares this object against the specified object.
     * The result is <code>true</code> if and only if the argument is
     * not <code>null</code> and it represents the same interface address as
     * this object.
     * <p>
     * Two instances of <code>InterfaceAddress</code> represent the same
     * address if the InetAddress, the prefix length and the broadcast are
     * the same for both.
     *
     * @param o the object to compare against.
     * @return <code>true</code> if the objects are the same;
     * <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InterfaceAddress)) return false;

        InterfaceAddress that = (InterfaceAddress) o;

        if (maskLength != that.maskLength) return false;
        if (!address.equals(that.address)) return false;
        if (broadcast != null ? !broadcast.equals(that.broadcast) : that.broadcast != null) return false;
        return addressClass == that.addressClass;
    }

    /**
     * Returns a hashcode for this Interface address.
     *
     * @return a hash code value for this Interface address.
     */
    @Override
    public int hashCode() {
        int result = address.hashCode();
        result = 31 * result + (broadcast != null ? broadcast.hashCode() : 0);
        result = 31 * result + (int) maskLength;
        result = 31 * result + addressClass.hashCode();
        return result;
    }

    /**
     * Converts this Interface address to a <code>String</code>. The
     * string returned is of the form: InetAddress / prefix length [ broadcast address ].
     *
     * @return a string representation of this Interface address.
     */
    @Override
    public String toString() {
        return "InterfaceAddress{" +
                "address=" + getAddress() +
                ", broadcast=" + getBroadcast() +
                ", maskLength=" + getMaskLength() +
                ", addressClass=" + getAddressClass() +
                '}';
    }

    /**
     * Get the network prefix length to use.
     * TODO Should be using some other method to determine the real subnet by querying the host somehow
     *
     * @return the network prefix length
     */
    private short getNetworkPrefixLength() {
        byte[] byteAddress = address.getAddress();

        // look at special networks and see if first bytes match
        for (Map.Entry<byte[], String> specialNetworkEntry : NETWORKS.entrySet()) {
            byte[] specialNetwork = specialNetworkEntry.getKey();
            String networkInfo = specialNetworkEntry.getValue();
            String[] networkInfoParts = networkInfo.split(":");
            short networkMaskLength = Short.parseShort(networkInfoParts[0]);
            short networkPrefixLength = Short.parseShort(networkInfoParts[1]);

            boolean special = true;
            for (int i = 0; i < specialNetwork.length; i++) {
                byte networkPartOfByteAddress = (byte) ((byteAddress[i] & (byte) (((i * 8) < networkMaskLength) ? 0xFF : (0xFF00 >> networkMaskLength % 8))));
                if (specialNetwork[i] != networkPartOfByteAddress) {
                    special = false;
                    break;
                }
            }
            if (special) {
                return networkPrefixLength;
            }
        }

        return getMaskLength();
    }

    /**
     * Get the network mask length to use based on its address class
     *
     * @return the mask length
     */
    private short getMaskLength() {
        switch (addressClass) {
            case CLASS_A:
                return 8; // 10.0.0.0/8
            case CLASS_B:
                return 12; // 172.16.0.0/12
            case CLASS_C:
                return 16; // 192.168.0.0/16
            case CLASS_D:
                return 24; // 224.0.0.0/24
            case IPV6_MULTICAST:
            case IPV6:
                return 8;  // ff00::0000.0000.0000.0000.0000.0000.0000/8
            default:
                return 0;  // 0.0.0.0/0
        }
    }

    /**
     * Determines the address class.  Uses some questionable
     * heuristics to guess the class based on properties of the given address.
     *
     * @return the guessed address class
     */
    private NetClass getAddressClass() {
        byte[] byteAddress = address.getAddress();
        int firstOctet = (byteAddress[0] & 0xFF);
        int secondOctet = (byteAddress[1] & 0xFF);
        int thirdOctet = (byteAddress[2] & 0xFF);

        if (byteAddress.length > 4) {
            if (firstOctet == 255) {
                return IPV6_MULTICAST;
            }
            return IPV6;
        }

        if (firstOctet == 10) {
            return CLASS_A;
        } else if (firstOctet == 172 && (secondOctet >= 16 && secondOctet < 32)) {
            return CLASS_B;
        } else if (firstOctet == 192 && secondOctet == 168) {
            return CLASS_C;
        } else if (firstOctet == 224 && secondOctet == 0 && thirdOctet == 0) {
            return CLASS_D;
        } else {
            return IPV4;
        }
    }

    /**
     * Get the broadcast address to use for a specified internet address based on the specified address class
     *
     * @return the broadcast address to use
     */
    private InetAddress getBroadcastAddress() {
        if (addressClass == IPV6 || addressClass == IPV6_MULTICAST || address.isLoopbackAddress()) {
            return null;
        }

        byte[] bytes = address.getAddress();
        byte[] newBytes = new byte[bytes.length];

        short networkPrefixLengthBytes = (byte) (networkPrefixLength / 8);
        byte networkPrefixMask = (byte) (0x00FF >> (networkPrefixLength % 8));

        for (int i = 0; i < bytes.length; i++) {
            if (i < networkPrefixLengthBytes) {
                newBytes[i] = bytes[i];
            } else if (i == networkPrefixLengthBytes) {
                newBytes[i] = (byte) (bytes[i] | networkPrefixMask);
            } else {
                newBytes[i] = (byte) 0xFF;
            }
        }
        try {
            return InetAddress.getByAddress(newBytes);
        } catch (UnknownHostException ignored) {
        }
        return null;
    }

}


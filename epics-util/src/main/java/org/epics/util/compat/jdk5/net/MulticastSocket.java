package org.epics.util.compat.jdk5.net;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class MulticastSocket extends java.net.MulticastSocket {
    /**
     * UDP maximum send message size (for sending search requests).
     * Calculation is specific to EPICS
     * MAX_UDP: 1500 (max of ethernet and 802.{2,3} MTU) - 20/40(IPv4/IPv6) - 8(UDP) - some reserve (e.g. IPSEC)
     * (the MTU of Ethernet is currently independent of its speed variant)
     */
    public static final int MAX_UDP_UNFRAGMENTED_SEND = 1440;

    private final Set<InetAddress> groups = new HashSet<InetAddress>();
    private InetSocketAddress inetSocketAddress;

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }

    public MulticastSocket(int port) throws IOException {
        super(port);
    }

    public MulticastSocket(SocketAddress bindaddr) throws IOException {
        super(bindaddr);
    }

    @Override
    public void connect(InetAddress address, int port) {
        super.connect(address, port);
        inetSocketAddress = new InetSocketAddress(address, port);
    }

    public void joinGroup(InetAddress mcastaddr) throws IOException {
        super.joinGroup(mcastaddr);
        this.groups.add(mcastaddr);
    }

    public void joinGroup(InetAddress mcastaddr, NetworkInterface netIf) throws IOException {
        super.setNetworkInterface(netIf);
        super.setLoopbackMode(true);
        super.joinGroup(mcastaddr);
        this.groups.add(mcastaddr);
    }

    @Override
    public void close() {
        for (InetAddress group : this.groups) {
            try {
                super.leaveGroup(group);
            } catch (IOException ignored) {
            }
        }
        super.close();
    }

    /**
     * Receive a datagram
     *
     * @param byteBuffer buffer to receive datagram into
     * @return the address datagram came from
     * @throws IOException if there was a problem
     */
    public InetSocketAddress receive(ByteBuffer byteBuffer) throws IOException {
        int maxPacketLength = byteBuffer.limit();
        DatagramPacket datagramPacket = new DatagramPacket(byteBuffer.array(), maxPacketLength);

        receive(datagramPacket);
        int bytesRead = datagramPacket.getLength();

        byteBuffer.put(datagramPacket.getData());
        byteBuffer.limit(bytesRead);

        return new InetSocketAddress(datagramPacket.getAddress(), datagramPacket.getPort());
    }

    public synchronized void send(ByteBuffer byteBuffer) throws IOException {
        if (!isConnected()) {
            throw new IOException("Socket must be connected to send buffer");
        }

        send(byteBuffer, getInetSocketAddress());
    }

    /**
     * Sends a datagram via this MulticastSocket.
     * <p>
     * The remaining bytes in the given buffer are transmitted as a single datagram to the given target address.
     * <p>
     * The datagram is transferred from the byte buffer as if by a regular write operation.
     *
     * @param byteBuffer    byte buffer to send
     * @param socketAddress the address to send to
     * @throws IOException if there is a problem sending
     */
    public void send(ByteBuffer byteBuffer, InetSocketAddress socketAddress) throws IOException {
        DatagramPacket datagramPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.limit(), socketAddress.getAddress(), socketAddress.getPort());
        send(datagramPacket);

        // All done, set new position
        byteBuffer.position(byteBuffer.limit());
    }
}

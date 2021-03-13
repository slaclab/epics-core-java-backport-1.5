package org.epics.util.compat.jdk5.net;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class MulticastSocket extends java.net.MulticastSocket {
    private NetworkInterface networkInterface;
    private final Set<InetSocketAddress> groups = new HashSet<InetSocketAddress>();
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

    /**
     * Join multicast group.
     *
     * @param mcastaddr the group to join.  No port is specified
     * @throws IOException if the group cannot be joined
     */
    @Override
    public void joinGroup(InetAddress mcastaddr) throws IOException {
        super.joinGroup(mcastaddr);
    }

    public void joinGroup(InetSocketAddress mcastaddr, NetworkInterface netIf) throws IOException {
        // Uncomment3 lines to join the group in the recommended way for JDK 1.5
        //        super.setLoopbackMode(true);
        //        setNetworkInterface(netIf);
        //        super.joinGroup(mcastaddr.getAddress());

        // Next line to join group in more up-to-date way
        super.joinGroup(mcastaddr, netIf.getNetworkInterface());

        this.groups.add(mcastaddr);
    }

    @Override
    public void close() {
        for (InetSocketAddress group : this.groups) {
            try {
                super.leaveGroup(group.getAddress());
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

    /**
     * Only set network interface if it supports multicast
     *
     * @param netIf network interface that supports multicast
     * @throws SocketException if does not support multicast
     */
    public void setNetworkInterface(NetworkInterface netIf) throws SocketException {
        if (netIf.supportsMulticast() && (this.networkInterface == null || this.networkInterface.equals(netIf))) {
            super.setNetworkInterface(netIf.getNetworkInterface());
            this.networkInterface = netIf;
            return;
        }
        throw new SocketException("Invalid argument.  The specified network interface does not support multicast or the socket is already assigned to a different network interface");
    }
}

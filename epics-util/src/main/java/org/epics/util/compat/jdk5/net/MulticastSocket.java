package org.epics.util.compat.jdk5.net;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

public class MulticastSocket extends java.net.MulticastSocket {
    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    private NetworkInterface networkInterface;
    private InetSocketAddress inetSocketAddress;

    public MulticastSocket() throws IOException {
        super();
    }

    public MulticastSocket(int port) throws IOException {
        super(port);
    }

    public MulticastSocket(SocketAddress bindaddr) throws IOException {
        super(bindaddr);
    }

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }

    @Override
    public void connect(InetAddress address, int port) {
        super.connect(address, port);
        inetSocketAddress = new InetSocketAddress(address, port);
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

    @Override
    public synchronized void receive(DatagramPacket packet) throws IOException {
        super.receive(packet);
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
        if (this.networkInterface == null || this.networkInterface.equals(netIf)) {
            super.setNetworkInterface(netIf.getNetworkInterface());
            this.networkInterface = netIf;
            return;
        }
        System.err.println("The specified network interface does not support multicast");
        throw new SocketException("Invalid argument.  The specified network interface does not support multicast or the socket is already assigned to a different network interface");
    }

    @Override
    public void setInterface(InetAddress inf) throws SocketException {
        super.setInterface(inf);
    }
}

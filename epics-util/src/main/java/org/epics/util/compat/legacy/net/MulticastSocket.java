package org.epics.util.compat.legacy.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
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

    public MulticastSocket(int port) throws IOException {
        super(port);
    }

    public MulticastSocket(SocketAddress bindaddr) throws IOException {
        super(bindaddr);
    }

    public void joinGroup(InetAddress mcastaddr) throws IOException {
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
        DatagramPacket datagramPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.limit());

        receive(datagramPacket);
        byteBuffer.put(datagramPacket.getData());

        return new InetSocketAddress(datagramPacket.getAddress(), datagramPacket.getPort());
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
        DatagramPacket datagramPacket;
        int offset = byteBuffer.arrayOffset();
        int length = byteBuffer.limit();

        // Send in chunks if greater than max un-fragmented size
        while (length > 0) {
            int chunkLength = Math.min(length, MAX_UDP_UNFRAGMENTED_SEND);

            datagramPacket = new DatagramPacket(byteBuffer.array(), offset, chunkLength, socketAddress.getAddress(), socketAddress.getPort());
            send(datagramPacket);

            length -= chunkLength;
            offset += chunkLength;
        }

        // All done, set new position
        byteBuffer.position(byteBuffer.limit());
    }
}

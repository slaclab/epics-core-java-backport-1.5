package org.epics.pvaccess.impl.remote.utils;

import org.epics.pvaccess.util.InetAddressUtil;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.epics.pvaccess.PVAConstants.*;
import static org.epics.pvaccess.util.InetAddressUtil.getFirstLoopbackNIF;
import static org.epics.pvaccess.util.InetAddressUtil.getMulticastGroup;

public class PVAForwarder {
    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("Binding to UDP socket at port " + PVA_BROADCAST_PORT);

        DatagramSocket receiveSocket = new DatagramSocket(PVA_BROADCAST_PORT);
        InetSocketAddress mcAddress = new InetSocketAddress(getMulticastGroup(), PVA_BROADCAST_PORT);
        System.out.println("MC Group:   " + mcAddress);

        NetworkInterface loNif = getFirstLoopbackNIF().getNetworkInterface();
        System.out.println("MC Loopback Network IF: " + loNif);

        MulticastSocket sendSocket = new MulticastSocket();
        sendSocket.setNetworkInterface(loNif);

        byte[] buffer = new byte[MAX_UDP_PACKET];
        InetAddress addr = null;

        do {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            receiveSocket.receive(packet);
            InetSocketAddress responseFrom = (InetSocketAddress) packet.getSocketAddress();

            if (responseFrom.getAddress().isLoopbackAddress()) {
                continue;
            }

            ByteBuffer receiveBuffer = ByteBuffer.wrap(packet.getData(), packet.getOffset(), packet.getLength());

            final Integer payloadSize = readHeader(receiveBuffer);
            if (payloadSize == null)
                continue;

            final Byte qosCode = readQosCode(receiveBuffer, payloadSize);
            if (qosCode == null)
                continue;

            addr = readAddress(receiveBuffer);
            if (addr == null)
                break;

            final Integer port = readPort(receiveBuffer);
            if (port == null)
                continue;

            // accept given address if explicitly specified by sender
            if (!addr.isAnyLocalAddress())
                responseFrom = new InetSocketAddress(addr, port);
            else
                responseFrom = new InetSocketAddress(responseFrom.getAddress(), port);

            // clear unicast flag
            receiveBuffer.put(PVA_MESSAGE_HEADER_SIZE + 4, (byte) (qosCode & ~0x80));

            // update response address
            receiveBuffer.position(PVA_MESSAGE_HEADER_SIZE + 8);
            InetAddressUtil.encodeAsIPv6Address(receiveBuffer, responseFrom.getAddress());

            // need to recreate a new packet, otherwise send does not work
            packet = new DatagramPacket(
                    packet.getData(),
                    packet.getLength(),
                    mcAddress.getAddress(), PVA_BROADCAST_PORT);

            sendSocket.send(packet);
        } while (addr != null);
    }

    /**
     * Read header from received buffer
     *
     * @param receiveBuffer received buffer
     * @return the size of the payload
     */
    private static Integer readHeader(ByteBuffer receiveBuffer) {
        if (receiveBuffer.remaining() < PVA_MESSAGE_HEADER_SIZE) {
            return null;
        }

        // first byte is PVA_MAGIC
        final byte magic = receiveBuffer.get();
        if (magic != PVA_MAGIC) {
            return null;
        }

        // second byte version - major/minor nibble
        // check only major version for compatibility
        receiveBuffer.get();

        final byte flags = receiveBuffer.get();
        if ((flags & 0x80) != 0) {
            // 7th bit is set
            receiveBuffer.order(ByteOrder.BIG_ENDIAN);
        } else {
            receiveBuffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        // command ID and payload
        final byte commandId = receiveBuffer.get();
        if (commandId != 3) {
            return null;
        }
        final int payloadSize = receiveBuffer.getInt();

        // control message check (skip message)
        if ((flags & 0x01) != 0) {
            return null;
        }
        return payloadSize;
    }

    /**
     * Read QoS code from buffer
     *
     * @param receiveBuffer received buffer
     * @param payloadSize   size of buffer
     * @return the QoS code
     */
    private static Byte readQosCode(ByteBuffer receiveBuffer, Integer payloadSize) {
        if (receiveBuffer.remaining() < payloadSize ||
                receiveBuffer.remaining() < (4 + 1 + 3 + 16 + 2)) {
            return null;
        }

        receiveBuffer.getInt();
        final byte qosCode = receiveBuffer.get();

        // reserved part
        receiveBuffer.get();
        receiveBuffer.getShort();
        return qosCode;
    }

    /**
     * Read address code from buffer
     *
     * @param receiveBuffer received buffer
     * @return the address
     */
    private static InetAddress readAddress(ByteBuffer receiveBuffer) {
        if (receiveBuffer.remaining() < 16) {
            return null;
        }
        // 128-bit IPv6 address
        byte[] byteAddress = new byte[16];
        receiveBuffer.get(byteAddress);

        // Extract internet address from received packet
        InetAddress addr;
        try {
            addr = InetAddress.getByAddress(byteAddress);
        } catch (UnknownHostException e) {
            System.err.println("Invalid address '" + new String(byteAddress) + "' in search response.");
            return null;
        }

        return addr;
    }

    /**
     * Read port code from buffer
     *
     * @param receiveBuffer received buffer
     * @return the port
     */
    private static Integer readPort(ByteBuffer receiveBuffer) {
        if (receiveBuffer.remaining() < 2) {
            return null;
        }
        return receiveBuffer.getShort() & 0xFFFF;
    }
}

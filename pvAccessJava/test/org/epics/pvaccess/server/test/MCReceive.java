package org.epics.pvaccess.server.test;

import java.net.DatagramPacket;
import java.net.MulticastSocket;

/**
 * Utilities to test Multicast support
 *
 * @author Matej Sekoranja
 */
public class MCReceive {

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    public static void main(String[] args) {
        try {
            MulticastSocket socket = new MulticastSocket(MCUtils.MC_GROUP);
//            MulticastSocket socket = new MulticastSocket(MCUtils.MC_PORT);
            socket.setInterface(MCUtils.getMCNetworkInterfaceAddress());
            socket.joinGroup(MCUtils.getMCAddress());

            byte[] buffer = new byte[1500];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                System.out.println("Received packet from: " + packet.getSocketAddress());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}

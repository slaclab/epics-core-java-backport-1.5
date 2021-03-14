package org.epics.pvaccess.server.test;

import org.epics.pvaccess.util.InetAddressUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

/**
 * Simple test to check whether multicast is supported on the host
 */
public class MulticastTest {
    public static void main(String[] args) {
        try {
            // First find out if there are any network interfaces that support multicast
            org.epics.util.compat.jdk5.net.NetworkInterface networkInterface = InetAddressUtil.getFirstMulticastNIF();
            if (networkInterface == null) {
                System.err.println("Multicast Test failed: Could not find any network interfaces that support multicast");
                return;
            }

            // Not set up the test parameters
            byte[] message = {'H', 'e', 'l', 'l', 'o'};
            NetworkInterface multicastNIF = networkInterface.getNetworkInterface();
            MulticastSocket multicastSocket = new MulticastSocket(6789);
            InetAddress multicastGroup = InetAddressUtil.getMulticastGroup();

            // Set loopback mode so that the messages loop back to the same socket so they can be read using the same socket object
            multicastSocket.setLoopbackMode(true);
            if (!multicastSocket.getLoopbackMode()) {
                System.err.println("Warning Loopback mode not supported");
            }

            System.out.print("Joining Multicast Group: " + multicastGroup + " ... ");
            multicastSocket.setNetworkInterface(multicastNIF);
            multicastSocket.joinGroup(multicastGroup);
            System.out.println("Done");

            // Send message
            String stringToSend = new String(message, "ASCII");
            System.out.print("Sending Message to Multicast Group: " + stringToSend + " ... ");
            DatagramPacket datagramPacketToSend = new DatagramPacket(message, message.length, multicastGroup, 6789);
            multicastSocket.send(datagramPacketToSend);
            System.out.println("Done");

            // get response
            System.out.print("Receiving Message from Multicast Group: " + multicastGroup + " ... ");
            byte[] buffer = new byte[message.length];
            DatagramPacket datagramPacketToReceive = new DatagramPacket(buffer, buffer.length);
            multicastSocket.receive(datagramPacketToReceive);

            String receivedString = new String(buffer, "ASCII");
            System.out.println("\"" + receivedString + "\" Received");

            // leave the group...
            multicastSocket.leaveGroup(multicastGroup);

            if (stringToSend.equals(receivedString)) {
                System.out.println("Multicast Test successful");
            } else {
                System.err.println("Multicast Test failed");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

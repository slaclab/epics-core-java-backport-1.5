package org.epics.pvaccess.server.test;

import org.epics.pvaccess.util.InetAddressUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Simple test to check whether multicast is supported on the host on its local interfaces
 */
public class MulticastTestWithResponse {
    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    public static void main(String[] args) {
        Set<org.epics.util.compat.jdk5.net.NetworkInterface> nifs = InetAddressUtil.getMulticastNIFs();

        // First find out if there are any network interfaces that support multicast
        if (nifs.isEmpty()) {
            System.err.println("Multicast Test failed: Could not find any loopback network interfaces that support multicast");
            return;
        }

        for (org.epics.util.compat.jdk5.net.NetworkInterface networkInterface : nifs) {
            NetworkInterface multicastNIF = networkInterface.getNetworkInterface();
            List<InetAddress> multicastInterfaceAddresses = Collections.list(multicastNIF.getInetAddresses());

            for (InetAddress multicastInterfaceAddress : multicastInterfaceAddresses) {
                try {
                    System.out.println();
                    System.out.println("Testing Multicast doing send and receive on each sock for Network Interface: " + networkInterface.getDisplayName() + " on network: " + multicastInterfaceAddress);
                    System.out.println("=======================================================");

                    // Now set up the test parameters
                    byte[] message = {'H', 'e', 'l', 'l', 'o'};
                    String stringToSend = new String(message, "ASCII");

                    // Create two sockets - one for a port the other
                    int PORT = 6789;
                    MulticastSocket firstSocket = new MulticastSocket(PORT);
                    MulticastSocket secondSocket = new MulticastSocket();
                    InetAddress multicastGroup = InetAddressUtil.getMulticastGroup();

                    // Set up first as receiver and second as sender
                    firstSocket.setInterface(multicastInterfaceAddress);
                    secondSocket.setNetworkInterface(multicastNIF);

                    System.out.print("Joining Multicast Group: " + multicastGroup + " ... ");
                    firstSocket.joinGroup(multicastGroup);
                    System.out.println("Done");

                    // Send message
                    System.out.print("Sending Message to Multicast Group: " + stringToSend + " ... ");
                    DatagramPacket packetToSend = new DatagramPacket(message, message.length, multicastGroup, PORT);
                    secondSocket.send(packetToSend);
                    System.out.println("Done");

                    // get response
                    System.out.print("Receiving Message from Multicast Group: " + multicastGroup + " ... ");
                    byte[] buffer = new byte[message.length];
                    DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
                    firstSocket.setSoTimeout(3000); // Wait up to 3 seconds
                    try {
                        firstSocket.receive(receivedPacket);
                    } catch (IOException e) {
                        System.err.println("Timeout waiting for data: " + e.getMessage());
                        receivedPacket.setLength(0);
                    }

                    InetAddress receivedFrom = receivedPacket.getAddress();
                    int receivedFromPort = receivedPacket.getPort();

                    String receivedString = new String(buffer, "ASCII");
                    if (receivedPacket.getLength() > 0) {
                        System.out.println("\"" + receivedString + "\" Received");
                    } else {
                        System.out.println("Nothing Received");
                    }

                    /////////////////
                    //////////////// Now response message
                    /////////////////

                    System.out.println();
                    System.out.println("Reversing Direction ");
                    System.out.println("=======================================================");

                    // Send message
                    System.out.print("Sending " + stringToSend + " response back to " + receivedFrom.getHostAddress() + ":" + receivedFromPort + " ... ");
                    DatagramPacket newPacketToSend = new DatagramPacket(message, message.length, receivedFrom, receivedFromPort);
                    firstSocket.send(newPacketToSend);
                    System.out.println("Done");

                    // get response
                    System.out.print("Receiving response Message: ... ");
                    DatagramPacket newReceivedPacket = new DatagramPacket(buffer, buffer.length);
                    secondSocket.setSoTimeout(3000); // Wait up to 3 seconds
                    try {
                        secondSocket.receive(newReceivedPacket);
                    } catch (IOException e) {
                        System.err.println("Timeout waiting for data: " + e.getMessage());
                        newReceivedPacket.setLength(0);
                    }

                    receivedString = new String(buffer, "ASCII");
                    if (newReceivedPacket.getLength() > 0) {
                        System.out.println("\"" + receivedString + "\" Received");
                    } else {
                        System.out.println("Nothing Received");
                    }

                    /////////////////
                    //////////////// End
                    /////////////////

                    // leave the group...
                    firstSocket.leaveGroup(multicastGroup);
                    secondSocket.close();
                    firstSocket.close();

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
    }
}

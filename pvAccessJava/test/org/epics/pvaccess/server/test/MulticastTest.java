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
public class MulticastTest {
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
                    System.out.println("Testing Multicast for Network Interface: " + networkInterface.getDisplayName() + " on network: " + multicastInterfaceAddress);
                    System.out.println("=======================================================");

                    // Now set up the test parameters
                    byte[] message = {'H', 'e', 'l', 'l', 'o'};
                    int PORT = 6789;
                    MulticastSocket receiveSocket = new MulticastSocket(PORT);
                    receiveSocket.setInterface(multicastInterfaceAddress);
                    MulticastSocket sendSocket = new MulticastSocket();
                    sendSocket.setNetworkInterface(multicastNIF);
                    InetAddress multicastGroup = InetAddressUtil.getMulticastGroup();

                    System.out.print("Joining Multicast Group: " + multicastGroup + " ... ");
                    receiveSocket.joinGroup(multicastGroup);
                    System.out.println("Done");

                    // Send message
                    String stringToSend = new String(message, "ASCII");
                    System.out.print("Sending Message to Multicast Group: " + stringToSend + " ... ");
                    DatagramPacket sentPacket = new DatagramPacket(message, message.length, multicastGroup, PORT);
                    sendSocket.send(sentPacket);
                    System.out.println("Done");

                    // get response
                    System.out.print("Receiving Message from Multicast Group: " + multicastGroup + " ... ");
                    byte[] buffer = new byte[message.length];
                    DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
                    receiveSocket.setSoTimeout(3000); // Wait up to 3 seconds
                    try {
                        receiveSocket.receive(receivedPacket);
                    } catch (IOException e) {
                        System.err.println("Timeout waiting for data: " + e.getMessage());
                        receivedPacket.setLength(0);
                    }

                    String receivedString = new String(buffer, "ASCII");
                    if (receivedPacket.getLength() > 0) {
                        System.out.println("\"" + receivedString + "\" Received");
                    } else {
                        System.out.println("Nothing Received");
                    }

                    // leave the group...
                    receiveSocket.leaveGroup(multicastGroup);
                    receiveSocket.close();
                    sendSocket.close();

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

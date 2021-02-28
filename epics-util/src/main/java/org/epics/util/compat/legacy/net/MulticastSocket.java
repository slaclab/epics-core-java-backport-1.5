package org.epics.util.compat.legacy.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;

public class MulticastSocket extends java.net.MulticastSocket {
    private final Set<InetAddress> groups = new HashSet<InetAddress>();

    public MulticastSocket() throws IOException {
        super();
    }

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
        for ( InetAddress group : this.groups ) {
            try {
                super.leaveGroup(group);
            } catch (IOException ignored) {
            }
        }
        super.close();
    }
}

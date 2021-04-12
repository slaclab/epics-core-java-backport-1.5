package org.epics.pvaccess.impl.remote.io;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public interface Poller {

    void add(SelectableChannel channel, PollEvents handler, int ops);

    void remove(SelectionKey key);

    void pollOne() throws IOException;

}

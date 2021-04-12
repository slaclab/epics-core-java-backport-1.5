package org.epics.pvaccess.impl.remote.codec;

public class ConnectionClosedException extends RuntimeException {
    private static final long serialVersionUID = 1393611748151786339L;

    public ConnectionClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionClosedException(String message) {
        super(message);
    }

}

package org.jdbi.v3;

import java.util.function.Supplier;

class LazyHandle implements Supplier<Handle>, AutoCloseable {
    private final DBI dbi;

    private volatile Handle handle;
    private volatile boolean closed = false;

    public LazyHandle(DBI dbi) {
        this.dbi = dbi;
    }

    public Handle get() {
        if (handle == null) {
            initHandle();
        }
        return handle;
    }

    private synchronized void initHandle() {
        if (handle == null) {
            if (closed) {
                throw new IllegalStateException("Handle is closed");
            }
            handle = dbi.open();
        }
    }

    public synchronized void close() {
        closed = true;
        if (handle != null) {
            handle.close();
        }
    }
}

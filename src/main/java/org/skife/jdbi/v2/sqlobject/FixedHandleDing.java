package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.Handle;

class FixedHandleDing implements HandleDing
{

    private final Handle handle;

    FixedHandleDing(Handle handle) {
        this.handle = handle;
    }

    public Handle getHandle()
    {
        return handle;
    }

    public void release(String name)
    {
    }

    public void retain(String name)
    {
    }

    public boolean isRetained()
    {
        return false;
    }
}

package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.Handle;

class ConstantHandleDing implements HandleDing
{

    private final Handle handle;

    ConstantHandleDing(Handle handle) {
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

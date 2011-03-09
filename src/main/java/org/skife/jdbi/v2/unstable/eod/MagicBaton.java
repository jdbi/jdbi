package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.Handle;

class MagicBaton
{
    private final Handle handle;

    MagicBaton(Handle handle) {
        this.handle = handle;
    }

    public Handle getHandle()
    {
        return handle;
    }

    public void retainHandle()
    {

    }

    public void releaseHandle()
    {

    }
}

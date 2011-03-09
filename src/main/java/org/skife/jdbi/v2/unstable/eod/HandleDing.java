package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.Handle;

interface HandleDing
{
    public Handle getHandle();

    public void release(String name);

    void retain(String name);
}

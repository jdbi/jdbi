package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.Handle;

interface HandleDing
{
    public Handle getHandle();

    public void release(String name);

    void retain(String name);
}

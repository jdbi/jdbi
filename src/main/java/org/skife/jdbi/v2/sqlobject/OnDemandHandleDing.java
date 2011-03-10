package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.util.HashSet;
import java.util.Set;

class OnDemandHandleDing implements HandleDing
{
    private final DBI dbi;
    private final ThreadLocal<LocalDing> threadDing = new ThreadLocal<LocalDing>();

    OnDemandHandleDing(DBI dbi)
    {
        this.dbi = dbi;
    }

    public Handle getHandle()
    {
        if (threadDing.get() == null) {
            threadDing.set(new LocalDing(dbi.open()));
        }
        return threadDing.get().getHandle();
    }

    public void retain(String name)
    {
        getHandle(); // need to ensure the local ding has been created as this is called before getHandle sometimes.
        threadDing.get().retain(name);
    }

    public void release(String name)
    {
        LocalDing ding = threadDing.get();
        if (ding == null) {
            return;
        }
        ding.release(name);

    }

    class LocalDing implements HandleDing {

        private final Set<String> retentions = new HashSet<String>();
        private final Handle handle;

        public LocalDing(Handle handle)
        {
            this.handle = handle;
        }

        public Handle getHandle()
        {
            return handle;
        }

        public void release(String name)
        {
            retentions.remove(name);
            if (retentions.isEmpty()) {
                threadDing.set(null);
                handle.close();
            }
        }

        public void retain(String name)
        {
            retentions.add(name);
        }

    }
}

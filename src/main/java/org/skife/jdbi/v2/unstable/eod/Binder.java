package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.Query;

class Binder
{
    private final Bind bind;
    private final int index;

    public Binder(Bind bind, int index)
    {
        this.bind = bind;

        this.index = index;
    }

    public void bind(Query q, Object[] args)
    {
        q.bind(bind.value(), args[index]);
    }
}

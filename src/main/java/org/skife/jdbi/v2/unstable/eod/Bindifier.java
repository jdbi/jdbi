package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.Query;

class Bindifier
{
    private final Bind   bind;
    private final int    param_idx;
    private final Binder binder;

    Bindifier(Bind bind, int param_idx, Binder binder)
    {
        this.bind = bind;
        this.param_idx = param_idx;
        this.binder = binder;
    }

    void bind(Query q, Object[] args)
    {
        binder.bind(q, bind, args[param_idx]);
    }
}

package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.Query;

class PrimitiveBindifier implements Binder
{
    public void bind(Query q, Bind bind, int index,  Object[] args)
    {
        q.bind(bind.value(), args[index]);
    }

}

package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.Query;

class DefaultObjectBinder implements Binder<Object>
{
    public void bind(Query q, Bind bind, Object arg)
    {
        q.bind(bind.value(), arg);
    }

}

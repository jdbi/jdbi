package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.Query;

public interface Binder
{
    public void bind(Query q, Bind bind, int index, Object[] args);
}

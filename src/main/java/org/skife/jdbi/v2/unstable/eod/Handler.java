package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.Handle;

public interface Handler
{
    public Object invoke(Handle h, Object[] args);

}

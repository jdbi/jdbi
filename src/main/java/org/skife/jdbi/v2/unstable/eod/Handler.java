package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.Handle;

interface Handler
{
    public Object invoke(Handle h, Object[] args);

}

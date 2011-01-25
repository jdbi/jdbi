package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.Handle;

class CloseHandler implements Handler
{
    public Object invoke(Handle h, Object[] args)
    {
        h.close();
        return null;
    }
}

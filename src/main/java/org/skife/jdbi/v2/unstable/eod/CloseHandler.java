package org.skife.jdbi.v2.unstable.eod;

import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.Handle;

public class CloseHandler implements Handler
{
    public Object invoke(Handle h, Object[] args)
    {
        h.close();
        return null;
    }
}

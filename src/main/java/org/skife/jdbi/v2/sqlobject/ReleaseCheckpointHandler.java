package org.skife.jdbi.v2.sqlobject;

import net.sf.cglib.proxy.MethodProxy;

class ReleaseCheckpointHandler implements Handler
{
    public Object invoke(HandleDing h, Object target, Object[] args, MethodProxy mp)
    {
        h.getHandle().release(String.valueOf(args[0]));
        return null;
    }
}

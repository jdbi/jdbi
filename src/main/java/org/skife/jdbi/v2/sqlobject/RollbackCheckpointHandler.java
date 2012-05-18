package org.skife.jdbi.v2.sqlobject;

import net.sf.cglib.proxy.MethodProxy;

class RollbackCheckpointHandler implements Handler
{
    public Object invoke(HandleDing h, Object target, Object[] args, MethodProxy mp)
    {
        h.getHandle().rollback(String.valueOf(args[0]));
        return null;
    }
}

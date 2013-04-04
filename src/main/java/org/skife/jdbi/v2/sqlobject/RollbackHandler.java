package org.skife.jdbi.v2.sqlobject;

import net.sf.cglib.proxy.MethodProxy;

class RollbackHandler implements Handler
{
    public Object invoke(HandleDing h, Object target, Object[] args, MethodProxy mp)
    {
        h.release("transaction#explicit");
        h.getHandle().rollback();
        return null;
    }
}

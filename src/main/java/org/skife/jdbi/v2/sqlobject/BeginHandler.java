package org.skife.jdbi.v2.sqlobject;

import net.sf.cglib.proxy.MethodProxy;

class BeginHandler implements Handler
{
    public Object invoke(HandleDing h, Object target, Object[] args, MethodProxy mp)
    {
        h.retain("transaction#explicit");
        h.getHandle().begin();
        return null;
    }
}

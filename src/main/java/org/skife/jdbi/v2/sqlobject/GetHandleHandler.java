package org.skife.jdbi.v2.sqlobject;

import net.sf.cglib.proxy.MethodProxy;

class GetHandleHandler implements Handler
{
    public Object invoke(HandleDing h, Object target, Object[] args, MethodProxy mp)
    {
        return h.getHandle();
    }
}

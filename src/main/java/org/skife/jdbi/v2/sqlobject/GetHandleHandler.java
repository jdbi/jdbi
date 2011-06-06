package org.skife.jdbi.v2.sqlobject;

class GetHandleHandler implements Handler
{
    public Object invoke(HandleDing h, Object target, Object[] args)
    {
        return h.getHandle();
    }
}

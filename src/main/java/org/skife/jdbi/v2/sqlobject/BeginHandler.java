package org.skife.jdbi.v2.sqlobject;

class BeginHandler implements Handler
{
    public Object invoke(HandleDing h, Object target, Object[] args)
    {
        h.retain("transaction");
        h.getHandle().begin();
        return null;
    }
}

package org.skife.jdbi.v2.sqlobject;

class ReleaseCheckpointHandler implements Handler
{
    public Object invoke(HandleDing h, Object target, Object[] args)
    {
        h.getHandle().release(String.valueOf(args[0]));
        return null;
    }
}

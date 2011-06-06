package org.skife.jdbi.v2.sqlobject;

class RollbackCheckpointHandler implements Handler
{
    public Object invoke(HandleDing h, Object target, Object[] args)
    {
        h.getHandle().rollback(String.valueOf(args[0]));
        return null;
    }
}

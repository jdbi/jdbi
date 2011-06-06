package org.skife.jdbi.v2.sqlobject;

class CheckpointHandler implements Handler
{
    public Object invoke(HandleDing h, Object target, Object[] args)
    {
        h.getHandle().checkpoint(String.valueOf(args[0]));
        return null;
    }
}

package org.skife.jdbi.v2.sqlobject;

class RollbackHandler implements Handler
{
    public Object invoke(HandleDing h, Object target, Object[] args)
    {
        h.release("transaction");
        h.getHandle().rollback();
        return null;
    }
}

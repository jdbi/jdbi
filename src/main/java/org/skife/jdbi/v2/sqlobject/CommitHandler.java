package org.skife.jdbi.v2.sqlobject;

class CommitHandler implements Handler
{
    public Object invoke(HandleDing h, Object target, Object[] args)
    {
        h.release("transaction");
        h.getHandle().commit();
        return null;
    }
}

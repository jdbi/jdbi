package org.skife.jdbi.v2.sqlobject;

class TransformHandler implements Handler
{
    public Object invoke(HandleDing h, final Object target, Object[] args)
    {
        Class t = (Class) args[0];
        return SqlObject.buildSqlObject(t, h);
    }
}

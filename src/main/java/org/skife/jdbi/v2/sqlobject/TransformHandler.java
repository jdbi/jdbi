package org.skife.jdbi.v2.sqlobject;

import net.sf.cglib.proxy.MethodProxy;

class TransformHandler implements Handler
{
    public Object invoke(HandleDing h, final Object target, Object[] args, MethodProxy mp)
    {
        Class t = (Class) args[0];
        return SqlObject.buildSqlObject(t, h);
    }
}

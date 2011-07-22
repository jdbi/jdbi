package org.skife.jdbi.v2.sqlobject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class HashCodeHandler implements Handler
{
    public Object invoke(final HandleDing h, final Object target, final Object[] args)
    {
        return System.identityHashCode(target);
    }

    static Map<Method, Handler> handler()
    {
        try
        {
            Map<Method, Handler> handler = new HashMap<Method, Handler>();
            handler.put(Object.class.getMethod("hashCode"), new HashCodeHandler());
            return handler;
        }
        catch (NoSuchMethodException e)
        {
            throw new IllegalStateException("JVM error");
        }
    }

}

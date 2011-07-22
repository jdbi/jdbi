package org.skife.jdbi.v2.sqlobject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ToStringHandler implements Handler
{
    public Object invoke(final HandleDing h, final Object target, final Object[] args)
    {
        return target.getClass().getName() + '@' + Integer.toHexString(target.hashCode());
    }

    static Map<Method, Handler> handler()
    {
        try
        {
            Map<Method, Handler> handler = new HashMap<Method, Handler>();
            handler.put(Object.class.getMethod("toString"), new ToStringHandler());
            return handler;
        }
        catch (NoSuchMethodException e)
        {
            throw new IllegalStateException("JVM error");
        }
    }

}

package org.skife.jdbi.v2.sqlobject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class EqualsHandler implements Handler
{
    public Object invoke(final HandleDing h, final Object target, final Object[] args)
    {
        // basic reference equals for now.
        return target == args[0];
    }

    static Map<Method, Handler> handler()
    {
        try
        {
            Map<Method, Handler> handler = new HashMap<Method, Handler>();
            handler.put(Object.class.getMethod("equals", Object.class), new EqualsHandler());
            return handler;
        }
        catch (NoSuchMethodException e)
        {
            throw new IllegalStateException("JVM error");
        }
    }

}

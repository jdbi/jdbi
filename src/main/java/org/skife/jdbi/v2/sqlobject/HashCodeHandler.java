package org.skife.jdbi.v2.sqlobject;

import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class HashCodeHandler implements Handler
{
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

    @Override
    public Object invoke(HandleDing h, Object target, Object[] args, MethodProxy mp)
    {
        return System.identityHashCode(target);
    }
}

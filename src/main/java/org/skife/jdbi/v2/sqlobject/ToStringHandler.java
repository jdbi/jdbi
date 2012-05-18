package org.skife.jdbi.v2.sqlobject;

import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class ToStringHandler implements Handler
{
    private final String className;

    ToStringHandler(String className)
    {
        this.className = className;
    }

    public Object invoke(final HandleDing h, final Object target, final Object[] args, MethodProxy mp)
    {
        return className + '@' + Integer.toHexString(target.hashCode());
    }

    static Map<Method, Handler> handler(String className)
    {
        try
        {
            Map<Method, Handler> handler = new HashMap<Method, Handler>();
            handler.put(Object.class.getMethod("toString"), new ToStringHandler(className));
            return handler;
        }
        catch (NoSuchMethodException e)
        {
            throw new IllegalStateException("JVM error");
        }
    }

}

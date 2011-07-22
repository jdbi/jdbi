package org.skife.jdbi.v2.sqlobject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class HashCodeHandler implements Handler
{
    public Object invoke(final HandleDing h, final Object target, final Object[] args)
    {
        // Because I'm too lazy to be clever, I stole/adapted this from:
        // http://www.ibm.com/developerworks/java/library/j-jtp05273/index.html
        int hash = 1;
        hash = hash * 31 + h.hashCode();
        for (Class<?> cls : target.getClass().getInterfaces())
            hash = hash * 31 + cls.hashCode();

        return hash;
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

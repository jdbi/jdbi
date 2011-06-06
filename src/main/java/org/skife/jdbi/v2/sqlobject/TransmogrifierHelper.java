package org.skife.jdbi.v2.sqlobject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class TransmogrifierHelper
{
    static Map<Method, Handler> handlers()
    {
        try {
            Map<Method, Handler> h = new HashMap<Method, Handler>();
            h.put(Transmogrifier.class.getMethod("become", Class.class), new TransformHandler());
            return h;
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("someone wonkered up the bytecode", e);
        }
    }
}

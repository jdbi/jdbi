package org.skife.jdbi.v2.sqlobject;

import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

class PassThroughHandler implements Handler
{

    private final Method method;

    PassThroughHandler(Method method)
    {
        this.method = method;
    }

    @Override
    public Object invoke(HandleDing h, Object target, Object[] args, MethodProxy mp)
    {
        try {
            return mp.invokeSuper(target, args);
        }
        catch (AbstractMethodError e) {
            throw new AbstractMethodError("Method " + method.getDeclaringClass().getName() + "#" + method.getName() +
                                               " doesn't make sense -- it probably needs a @Sql* annotation of some kind.");
        }
        catch (Throwable throwable) {
            /*

             */
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            else if (throwable instanceof Error) {
                throw (Error) throwable;
            }
            else {
                throw new RuntimeException(throwable);
            }
        }
    }
}

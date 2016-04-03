/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.sqlobject;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import net.sf.cglib.proxy.MethodProxy;

import org.jdbi.v3.Handle;

class PassThroughHandler implements Handler
{

    private final Method method;

    PassThroughHandler(Method method)
    {
        this.method = method;
    }

    @Override
    public Object invoke(Supplier<Handle> handle, Object target, Object[] args, MethodProxy mp)
    {
        try {
            if (method.isDefault()) {
                // TERRIBLE, HORRIBLE, NO GOOD, VERY BAD HACK
                // Courtesy of:
                // https://rmannibucau.wordpress.com/2014/03/27/java-8-default-interface-methods-and-jdk-dynamic-proxies/

                // CGLIB does not yet support calling interface default methods through mp.invokeSuper().

                // We can use MethodHandles to look up and invoke the super method, but since this class is not an
                // implementation of method.getDeclaringClass(), MethodHandles.Lookup will throw an exception since
                // this class doesn't have access to the super method, according to Java's access rules. This horrible,
                // awful workaround allows us to directly invoke MethodHandles.Lookup's private constructor, bypassing
                // the usual access checks.

                // We should get rid of this workaround as soon as a viable alternative exists.

                final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                if (!constructor.isAccessible()) {
                    constructor.setAccessible(true);
                }

                Class<?> declaringClass = method.getDeclaringClass();
                return constructor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE) // *shudder*
                        .unreflectSpecial(method, declaringClass)
                        .bindTo(target)
                        .invokeWithArguments(args);
            }

            return mp.invokeSuper(target, args);
        }
        catch (AbstractMethodError e) {
            throw new AbstractMethodError("Method " + method.getDeclaringClass().getName() + "#" + method.getName() +
                                               " doesn't make sense -- it probably needs a @Sql* annotation of some kind.");
        }
        catch (Throwable throwable) {
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

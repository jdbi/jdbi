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
package org.jdbi.v3.core;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.jdbi.v3.core.internal.JdbiThreadLocals;
import org.jdbi.v3.core.internal.Throwables;
import org.jdbi.v3.core.internal.UtilityClassException;

class OnDemandExtensions {
    private static final Method EQUALS_METHOD;
    private static final Method HASHCODE_METHOD;
    private static final Method TOSTRING_METHOD;

    static {
        try {
            EQUALS_METHOD = Object.class.getMethod("equals", Object.class);
            HASHCODE_METHOD = Object.class.getMethod("hashCode");
            TOSTRING_METHOD = Object.class.getMethod("toString");
        } catch (NoSuchMethodException wat) {
            throw new IllegalStateException("JVM error", wat);
        }
    }

    private OnDemandExtensions() {
        throw new UtilityClassException();
    }

    static <E> E create(Jdbi db, Class<E> extensionType) {
        ThreadLocal<E> threadExtension = new ThreadLocal<>();

        InvocationHandler handler = (proxy, method, args) -> {
            if (EQUALS_METHOD.equals(method)) {
                return proxy == args[0];
            }

            if (HASHCODE_METHOD.equals(method)) {
                return System.identityHashCode(proxy);
            }

            if (TOSTRING_METHOD.equals(method)) {
                return extensionType + "@" + Integer.toHexString(System.identityHashCode(proxy));
            }

            if (threadExtension.get() != null) {
                return invoke(threadExtension.get(), method, args);
            }
            return db.withExtension(extensionType, extension ->
                    JdbiThreadLocals.invokeInContext(threadExtension, extension,
                            () -> invoke(extension, method, args)));
        };

        return extensionType.cast(
                Proxy.newProxyInstance(
                        extensionType.getClassLoader(),
                        new Class[]{extensionType}, handler));
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Exception {
        return Throwables.throwingOnlyException(() -> {
            if (Proxy.isProxyClass(target.getClass())) {
                return Proxy.getInvocationHandler(target).invoke(target, method, args);
            } else {
                return MethodHandles.lookup().unreflect(method).bindTo(target).invokeWithArguments(args);
            }
        });
    }
}

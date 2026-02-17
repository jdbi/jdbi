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
package org.jdbi.spring;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.internal.UtilityClassException;
import org.jdbi.core.internal.exceptions.Sneaky;
import org.jdbi.sqlobject.SqlObject;

import static org.jdbi.core.internal.JdbiClassUtils.EQUALS_METHOD;
import static org.jdbi.core.internal.JdbiClassUtils.HASHCODE_METHOD;
import static org.jdbi.core.internal.JdbiClassUtils.TOSTRING_METHOD;

class JdbiJtaBinder {
    private JdbiJtaBinder() {
        throw new UtilityClassException();
    }

    /**
     * Proxies the extension object to bind it to the jta framework. Creates and closes the handle if needed.
     */
    static <E> E bind(Jdbi jdbi, Class<E> extensionType) {
        InvocationHandler invocationHandler = createInvocationHandler(jdbi, extensionType);
        return extensionType.cast(createProxy(invocationHandler, extensionType, SqlObject.class));
    }

    private static InvocationHandler createInvocationHandler(Jdbi jdbi, Class<?> extensionType) {
        return (proxy, method, args) -> {
            Handle handle = JdbiUtil.getHandle(jdbi);
            try {
                Object delegate = handle.attach(extensionType);
                return invoke(delegate, method, args);
            } finally {
                JdbiUtil.closeIfNeeded(handle);
            }
        };
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static Object createProxy(InvocationHandler naiveHandler, Class<?> extensionType, Class<?>... extraTypes) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (EQUALS_METHOD.equals(method)) {
                return proxy == args[0];
            }

            if (HASHCODE_METHOD.equals(method)) {
                return System.identityHashCode(proxy);
            }

            if (TOSTRING_METHOD.equals(method)) {
                return "JdbiJta on demand proxy for " + extensionType.getName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
            }

            return naiveHandler.invoke(proxy, method, args);

        };

        Class<?>[] types = Stream.of(
                    Stream.of(extensionType),
                    Arrays.stream(extensionType.getInterfaces()),
                    Arrays.stream(extraTypes))
                .flatMap(Function.identity())
                .distinct()
                .toArray(Class[]::new);
        return Proxy.newProxyInstance(extensionType.getClassLoader(), types, handler);
    }

    private static Object invoke(Object target, Method method, Object[] args) {
        try {
            if (Proxy.isProxyClass(target.getClass())) {
                return Proxy.getInvocationHandler(target)
                        .invoke(target, method, args);
            } else {
                return MethodHandles.lookup().unreflect(method)
                        .bindTo(target)
                        .invokeWithArguments(args);
            }
        } catch (Throwable t) {
            throw Sneaky.throwAnyway(t);
        }
    }

}

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
package org.jdbi.v3.core.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.internal.exceptions.Unchecked;

public class OnDemandExtensions implements JdbiConfig<OnDemandExtensions> {
    private static final Method EQUALS_METHOD;
    private static final Method HASHCODE_METHOD;
    private static final Method TOSTRING_METHOD;

    private Factory factory;

    static {
        try {
            EQUALS_METHOD = Object.class.getMethod("equals", Object.class);
            HASHCODE_METHOD = Object.class.getMethod("hashCode");
            TOSTRING_METHOD = Object.class.getMethod("toString");
        } catch (NoSuchMethodException wat) {
            throw new IllegalStateException("OnDemandExtensions initialization failed", wat);
        }
    }

    public OnDemandExtensions() {
        factory = (d, x, t) -> Optional.empty();
    }

    private OnDemandExtensions(OnDemandExtensions other) {
        factory = other.factory;
    }

    public OnDemandExtensions setFactory(Factory factory) {
        this.factory = factory;
        return this;
    }

    public <E> E create(Jdbi db, Class<E> extensionType, Class<?>... extraTypes) {
        return extensionType.cast(
               factory.onDemand(db, extensionType, extraTypes)
                  .orElseGet(() -> createProxy(db, extensionType, extraTypes)));
    }

    private Object createProxy(Jdbi db, Class<?> extensionType, Class<?>... extraTypes) {
        db.getConfig(Extensions.class).onCreateProxy();
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

            return db.withExtension(extensionType, extension -> invoke(extension, method, args));
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
        if (Proxy.isProxyClass(target.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(target);
            return Unchecked.<Object[], Object>function(params -> handler.invoke(target, method, params)).apply(args);
        } else {
            MethodHandle handle = Unchecked.function(MethodHandles.lookup()::unreflect).apply(method).bindTo(target);
            return Unchecked.<Object[], Object>function(handle::invokeWithArguments).apply(args);
        }
    }

    @Override
    public OnDemandExtensions createCopy() {
        return new OnDemandExtensions(this);
    }

    @FunctionalInterface
    public interface Factory {
        Optional<Object> onDemand(Jdbi db, Class<?> extensionType, Class<?>... extraTypes);
    }
}

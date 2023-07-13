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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.internal.exceptions.Sneaky;

import static org.jdbi.v3.core.internal.JdbiClassUtils.EQUALS_METHOD;
import static org.jdbi.v3.core.internal.JdbiClassUtils.HASHCODE_METHOD;
import static org.jdbi.v3.core.internal.JdbiClassUtils.TOSTRING_METHOD;

public class OnDemandExtensions implements JdbiConfig<OnDemandExtensions> {
    private Factory onDemandExtensionFactory;

    public OnDemandExtensions() {
        onDemandExtensionFactory = (jdbi, extensionType, extraTypes) -> Optional.empty();
    }

    private OnDemandExtensions(OnDemandExtensions other) {
        onDemandExtensionFactory = other.onDemandExtensionFactory;
    }

    public OnDemandExtensions setFactory(Factory onDemandExtensionFactory) {
        this.onDemandExtensionFactory = onDemandExtensionFactory;
        return this;
    }

    public <E> E create(Jdbi jdbi, Class<E> extensionType, Class<?>... extraTypes) {
        return extensionType.cast(
               onDemandExtensionFactory.onDemand(jdbi, extensionType, extraTypes)
                  .orElseGet(() -> createProxy(jdbi, extensionType, extraTypes)));
    }

    private Object createProxy(Jdbi jdbi, Class<?> extensionType, Class<?>... extraTypes) {
        jdbi.getConfig(Extensions.class).onCreateProxy();

        InvocationHandler handler = (proxy, method, args) -> {
            if (EQUALS_METHOD.equals(method)) {
                return proxy == args[0];
            }

            if (HASHCODE_METHOD.equals(method)) {
                return System.identityHashCode(proxy);
            }

            if (TOSTRING_METHOD.equals(method)) {
                return "Jdbi on demand proxy for " + extensionType.getName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
            }

            return jdbi.withExtension(extensionType, extension -> invoke(extension, method, args));
        };

        var types = new LinkedHashSet<Class<?>>();
        types.add(extensionType);
        types.addAll(Arrays.asList(extensionType.getInterfaces()));
        types.addAll(Arrays.asList(extraTypes));
        return Proxy.newProxyInstance(extensionType.getClassLoader(), types.toArray(new Class<?>[0]), handler);
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

    @Override
    public OnDemandExtensions createCopy() {
        return new OnDemandExtensions(this);
    }

    @FunctionalInterface
    public interface Factory {
        Optional<Object> onDemand(Jdbi jdbi, Class<?> extensionType, Class<?>... extraTypes);
    }
}

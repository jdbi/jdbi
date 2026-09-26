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
package org.jdbi.v3.core.mapper.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.internal.ConfigCache;
import org.jdbi.v3.core.config.internal.ConfigCaches;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.core.internal.exceptions.Unchecked;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

class StaticMethodInstanceFactory<T> extends InstanceFactory<T> {
    private static final ConfigCache<Method, MethodHandle> HANDLE_CACHE =
            ConfigCaches.declare(JdbiClassUtils::unreflect);

    private final Class<T> type;
    private final Method method;

    StaticMethodInstanceFactory(Class<T> type, Method method) {
        super(method);
        this.type = requireNonNull(type, "type is null");
        this.method = requireNonNull(method, "method is null");
        if (!isStaticFactoryMethodFor(method, type)) {
            throw new IllegalArgumentException(format("Given method \"%s\" is not a valid factory method for %s", method, type));
        }
    }

    private static boolean isStaticFactoryMethodFor(Method method, Class<?> type) {
        return Modifier.isStatic(method.getModifiers())
            && type.isAssignableFrom(method.getReturnType());
    }

    @Override
    Function<Object[], T> instantiator(ConfigRegistry config) {
        final MethodHandle handle = HANDLE_CACHE.get(method, config);
        return Unchecked.function(params -> type.cast(handle.invokeWithArguments(params)));
    }

    @Override
    public String toString() {
        return method.toString();
    }
}

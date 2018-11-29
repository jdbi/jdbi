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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.jdbi.v3.core.internal.Throwables.throwingOnlyUnchecked;

class StaticMethodInstanceFactory<T> extends InstanceFactory<T> {
    private final Class<T> type;
    private final Method method;

    StaticMethodInstanceFactory(Class<T> type, Method method) {
        super(method);
        this.type = requireNonNull(type, "type is null");
        requireNonNull(method, "method is null");
        if (!isStaticFactoryMethodFor(method, type)) {
            throw new IllegalArgumentException(format("Given method \"%s\" is not a valid factory method for %s", method, type));
        }
        this.method = method;
    }

    private static boolean isStaticFactoryMethodFor(Method method, Class<?> type) {
        return Modifier.isStatic(method.getModifiers())
            && type.isAssignableFrom(method.getReturnType());
    }

    @Override
    T newInstance(Object... params) {
        return throwingOnlyUnchecked(() -> type.cast(method.invoke(null, params)));
    }

    @Override
    public String toString() {
        return method.toString();
    }
}

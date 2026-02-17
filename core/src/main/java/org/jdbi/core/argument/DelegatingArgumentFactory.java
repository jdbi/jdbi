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
package org.jdbi.core.argument;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.core.argument.internal.StatementBinder;
import org.jdbi.core.argument.internal.strategies.LoggableBinderArgument;
import org.jdbi.core.config.ConfigRegistry;

import static org.jdbi.core.generic.GenericTypes.getErasedType;

abstract class DelegatingArgumentFactory implements ArgumentFactory.Preparable {
    private final IdentityHashMap<Class<?>, Function<Object, Argument>> builders = new IdentityHashMap<>();

    @Override
    public Optional<Function<Object, Argument>> prepare(Type type, ConfigRegistry config) {
        return Optional.ofNullable(builders.get(getErasedType(type)));
    }

    @Override
    public Optional<Argument> build(Type expectedType, Object value, ConfigRegistry config) {
        Class<?> expectedClass = getErasedType(expectedType);

        if (value != null && expectedClass == Object.class) {
            expectedClass = value.getClass();
        }

        return Optional.ofNullable(builders.get(expectedClass)).map(r -> r.apply(value));
    }

    /**
     * @deprecated no longer used
     */
    @Override
    @Deprecated(since = "3.39.0", forRemoval = true)
    public Collection<? extends Type> prePreparedTypes() {
        return builders.keySet();
    }

    @SuppressWarnings("unchecked")
    <T> void register(Class<T> klass, int sqlType, StatementBinder<T> binder) {
        builders.put(klass,
            value -> value == null
                ? new NullArgument(sqlType)
                : new LoggableBinderArgument<>((T) value, binder));
    }
}

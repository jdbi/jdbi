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
package org.jdbi.v3.core.argument;

import java.lang.reflect.Type;
import java.sql.Types;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.jdbi.v3.core.config.ConfigRegistry;

import static org.jdbi.v3.core.generic.GenericTypes.findGenericParameter;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

class OptionalArgumentFactory implements ArgumentFactory {
    private static final Map<Class<?>, ArgBuilder<?>> BUILDERS = createInternalBuilders();

    private static <T> void register(Map<Class<?>, ArgBuilder<?>> map, Class<T> klass, int type, StatementBinder<T> binder) {
        map.put(klass, (ArgBuilder<T>) v -> new BinderArgument<>(klass, type, binder, v));
    }

    private static Map<Class<?>, ArgBuilder<?>> createInternalBuilders() {
        final Map<Class<?>, ArgBuilder<?>> map = new IdentityHashMap<>();

        register(map, OptionalInt.class, Types.INTEGER, (p, i, v) -> {
            if (v.isPresent()) {
                p.setInt(i, v.getAsInt());
            } else {
                p.setNull(i, Types.INTEGER);
            }
        });
        register(map, OptionalLong.class, Types.BIGINT, (p, i, v) -> {
            if (v.isPresent()) {
                p.setLong(i, v.getAsLong());
            } else {
                p.setNull(i, Types.BIGINT);
            }
        });
        register(map, OptionalDouble.class, Types.DOUBLE, (p, i, v) -> {
            if (v.isPresent()) {
                p.setDouble(i, v.getAsDouble());
            } else {
                p.setNull(i, Types.DOUBLE);
            }
        });

        return Collections.unmodifiableMap(map);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Argument> build(Type expectedType, Object value, ConfigRegistry config) {
        Class<?> expectedClass = getErasedType(expectedType);

        if (value != null && expectedClass == Object.class) {
            expectedClass = value.getClass();
        }

        @SuppressWarnings("rawtypes")
        ArgBuilder v = BUILDERS.get(expectedClass);

        if (v != null) {
            return Optional.of(v.build(value));
        }

        if (value instanceof Optional) {
            Object nestedValue = ((Optional<?>) value).orElse(null);
            Type nestedType = findOptionalType(expectedType, nestedValue);
            return config.get(Arguments.class).findFor(nestedType, nestedValue);
        } else {
            return Optional.empty();
        }
    }

    private Type findOptionalType(Type wrapperType, Object nestedValue) {
        if (getErasedType(wrapperType).equals(Optional.class)) {
            Optional<Type> nestedType = findGenericParameter(wrapperType, Optional.class);
            if (nestedType.isPresent()) {
                return nestedType.get();
            }
        }
        return nestedValue == null ? Object.class : nestedValue.getClass();
    }
}

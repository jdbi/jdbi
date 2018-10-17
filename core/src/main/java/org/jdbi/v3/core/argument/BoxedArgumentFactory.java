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
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import org.jdbi.v3.core.config.ConfigRegistry;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

class BoxedArgumentFactory implements ArgumentFactory {
    private static final Map<Class<?>, ArgBuilder<?>> BUILDERS = createInternalBuilders();

    private static <T> void register(Map<Class<?>, ArgBuilder<?>> map, Class<T> klass, int type, StatementBinder<T> binder) {
        map.put(klass, (ArgBuilder<T>) v -> new BinderArgument<>(klass, type, binder, v));
    }

    private static <T> StatementBinder<T> stringifyValue(StatementBinder<String> real) {
        return (p, i, v) -> real.bind(p, i, String.valueOf(v));
    }

    private static Map<Class<?>, ArgBuilder<?>> createInternalBuilders() {
        final Map<Class<?>, ArgBuilder<?>> map = new IdentityHashMap<>();

        register(map, Boolean.class, Types.BOOLEAN, PreparedStatement::setBoolean);
        register(map, Byte.class, Types.TINYINT, PreparedStatement::setByte);
        register(map, Character.class, Types.CHAR, stringifyValue(PreparedStatement::setString));
        register(map, Short.class, Types.SMALLINT, PreparedStatement::setShort);
        register(map, Integer.class, Types.INTEGER, PreparedStatement::setInt);
        register(map, Long.class, Types.INTEGER, PreparedStatement::setLong);
        register(map, Float.class, Types.FLOAT, PreparedStatement::setFloat);
        register(map, Double.class, Types.DOUBLE, PreparedStatement::setDouble);

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

        return Optional.ofNullable(v).map(f -> f.build(value));
    }
}

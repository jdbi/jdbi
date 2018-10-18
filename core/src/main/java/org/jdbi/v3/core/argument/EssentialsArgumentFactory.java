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
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.config.ConfigRegistry;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

class EssentialsArgumentFactory implements ArgumentFactory {
    private final Map<Class<?>, ArgBuilder<?>> builders = new IdentityHashMap<>();

    EssentialsArgumentFactory() {
        register(BigDecimal.class, Types.NUMERIC, PreparedStatement::setBigDecimal);
        register(byte[].class, Types.VARBINARY, PreparedStatement::setBytes);
        register(String.class, BinderArgument.builder(String.class, Types.VARCHAR, PreparedStatement::setString));
        register(UUID.class, Types.VARCHAR, PreparedStatement::setObject);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Argument> build(Type expectedType, Object value, ConfigRegistry config) {
        Class<?> expectedClass = getErasedType(expectedType);

        if (value != null && expectedClass == Object.class) {
            expectedClass = value.getClass();
        }

        @SuppressWarnings("rawtypes")
        ArgBuilder v = builders.get(expectedClass);

        return Optional.ofNullable(v).map(f -> f.build(value));
    }

    private <T> void register(Class<T> klass, int type, StatementBinder<T> binder) {
        register(klass, v -> new BinderArgument<>(klass, type, binder, v));
    }

    private <T> void register(Class<T> klass, ArgBuilder<T> builder) {
        builders.put(klass, builder);
    }
}

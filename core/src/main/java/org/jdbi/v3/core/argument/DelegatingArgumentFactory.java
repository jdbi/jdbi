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
import java.sql.SQLException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

abstract class DelegatingArgumentFactory implements ArgumentFactory {
    private final Map<Class<?>, Function<?, Argument>> builders = new IdentityHashMap<>();

    @Override
    public Optional<Argument> build(Type expectedType, Object value, ConfigRegistry config) {
        Class<?> expectedClass = getErasedType(expectedType);

        if (value != null && expectedClass == Object.class) {
            expectedClass = value.getClass();
        }

        // we can assume argbuilder to be compatible with value because register method is strict
        @SuppressWarnings("unchecked")
        Function<Object, Argument> reusable = (Function<Object, Argument>) builders.get(expectedClass);

        return Optional.ofNullable(reusable).map(r -> r.apply(value));
    }

    // this ensures our OOTB argument factories produce loggable, null-safe Arguments
    <T> void register(Class<T> klass, int sqlType, StatementBinder<T> binder) {
        builders.put(klass, (T value) -> new LoggableNullTolerantArgument<>(value, sqlType, binder));
    }

    private static class LoggableNullTolerantArgument<T> implements Argument {
        private final T value;
        private final int sqlType;
        private final StatementBinder<T> binder;

        private LoggableNullTolerantArgument(T value, int sqlType, StatementBinder<T> binder) {
            this.value = value;
            this.sqlType = sqlType;
            this.binder = binder;
        }

        @Override
        public void apply(int pos, PreparedStatement stmt, StatementContext ctx) throws SQLException {
            if (value == null) {
                stmt.setNull(pos, sqlType);
            } else {
                binder.bind(stmt, pos, value);
            }
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}

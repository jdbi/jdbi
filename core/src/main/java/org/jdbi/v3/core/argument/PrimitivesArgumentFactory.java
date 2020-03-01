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
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jdbi.v3.core.argument.internal.StatementBinder;
import org.jdbi.v3.core.argument.internal.strategies.LoggableBinderArgument;
import org.jdbi.v3.core.config.ConfigRegistry;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

// TODO this factory isn't actually preparable anymore
class PrimitivesArgumentFactory extends DelegatingArgumentFactory {
    private final Map<Class<?>, BiFunction<Object, ConfigRegistry, Argument>> builders = new IdentityHashMap<>();

    PrimitivesArgumentFactory() {
        register(boolean.class, Types.BOOLEAN, PreparedStatement::setBoolean);
        register(byte.class, Types.TINYINT, PreparedStatement::setByte);
        register(char.class, Types.CHAR, new ToStringBinder<>(PreparedStatement::setString));
        register(short.class, Types.SMALLINT, PreparedStatement::setShort);
        register(int.class, Types.INTEGER, PreparedStatement::setInt);
        register(long.class, Types.INTEGER, PreparedStatement::setLong);
        register(float.class, Types.FLOAT, PreparedStatement::setFloat);
        register(double.class, Types.DOUBLE, PreparedStatement::setDouble);
    }

    @Override
    public Optional<Argument> build(Type expectedType, Object value, ConfigRegistry config) {
        Class<?> expectedClass = getErasedType(expectedType);

        if (value != null && expectedClass == Object.class) {
            expectedClass = value.getClass();
        }

        return Optional.ofNullable(builders.get(expectedClass)).map(r -> r.apply(value, config));
    }

    @Override
    public Optional<Function<Object, Argument>> prepare(Type type, ConfigRegistry config) {
        return Optional.empty();
    }

    @Override
    public Collection<? extends Type> prePreparedTypes() {
        return Collections.emptySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    <T> void register(Class<T> klass, int sqlType, StatementBinder<T> binder) {
        builders.put(klass, (value, config) -> {
            if (value != null) {
                return new LoggableBinderArgument<>((T) value, binder);
            }

            if (config.get(Arguments.class).isBindingNullToPrimitivesPermitted()) {
                return new NullArgument(sqlType);
            }

            throw new IllegalArgumentException(String.format(
                "binding null to a primitive %s is forbidden by configuration, declare a boxed type instead", klass.getSimpleName()
            ));
        });
    }
}

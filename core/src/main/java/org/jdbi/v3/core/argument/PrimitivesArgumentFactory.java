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
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;

class PrimitivesArgumentFactory extends DelegatingArgumentFactory {
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
    public Optional<Function<Object, Argument>> prepare(Type type, ConfigRegistry config) {
        return super.prepare(type, config)
                .map(prepared -> value -> prepared.apply(checkForNull(config, type, value)));
    }
    @Override
    public Optional<Argument> build(Type expectedType, Object value, ConfigRegistry config) {
        return super.build(expectedType, checkForNull(config, expectedType, value), config);
    }

    private Object checkForNull(ConfigRegistry cfg, Type type, Object value) {
        if (value == null && !cfg.get(Arguments.class).isBindingNullToPrimitivesPermitted()) {
            throw new IllegalArgumentException(String.format(
                    "binding null to a primitive %s is forbidden by configuration, declare a boxed type instead", type
            ));
        }
        return value;
    }
}

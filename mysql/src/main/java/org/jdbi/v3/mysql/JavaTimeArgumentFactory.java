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
package org.jdbi.v3.mysql;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.NullArgument;
import org.jdbi.v3.core.argument.ObjectArgument;
import org.jdbi.v3.core.argument.internal.strategies.LoggableBinderArgument;
import org.jdbi.v3.core.config.ConfigRegistry;

/**
 * MySQL specific argument factory for Java Time types.
 * <br>
 * {@link Instant} must be mapped to {@link Types#TIMESTAMP}, as the MySQL driver does not support them with {@link PreparedStatement#setObject}.
 *
 * @since 3.52.0
 */
final class JavaTimeArgumentFactory implements ArgumentFactory.Preparable {

    private static final Map<Class<?>, Function<Object, Argument>> TYPES = Map.of(
        Instant.class, value -> value == null
            ? new NullArgument(Types.TIMESTAMP)
            : new LoggableBinderArgument<>(value, (p, i, v) -> p.setTimestamp(i, Timestamp.from((Instant) v))),
        OffsetDateTime.class, value -> value == null
            ? new NullArgument(Types.TIMESTAMP)
            : ObjectArgument.of(value, Types.TIMESTAMP),
        ZonedDateTime.class, value -> value == null
            ? new NullArgument(Types.TIMESTAMP)
            : ObjectArgument.of(((ZonedDateTime) value).toOffsetDateTime(), Types.TIMESTAMP)
    );

    @Override
    public Optional<Function<Object, Argument>> prepare(Type type, ConfigRegistry config) {
        return Optional.of(type)
            .filter(Class.class::isInstance)
            .map(Class.class::cast)
            .map(TYPES::get);
    }
}

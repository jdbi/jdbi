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

import java.sql.PreparedStatement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.jdbi.v3.core.argument.internal.StatementBinder;
import org.jdbi.v3.core.argument.internal.strategies.LoggableBinderArgument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.meta.Legacy;

/**
 * Supports alternative argument bindings for types by using the {@link Legacy} qualifier.
 * <br>
 * Restores the non-{@link PreparedStatement#setObject} behavior for java time types. This is a fallback
 * when using {@link PreparedStatement#setObject} is not available or does not work with a JDBC driver.
 *
 * @since 3.52.0
 */
final class LegacyArgumentFactory implements QualifiedArgumentFactory.Preparable {

    private final ConcurrentMap<QualifiedType<?>, Function<Object, Argument>> builders = new ConcurrentHashMap<>();

    LegacyArgumentFactory() {
        register(Instant.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.from(v)));
        register(LocalDate.class, Types.DATE, (p, i, v) -> p.setDate(i, java.sql.Date.valueOf(v)));
        register(LocalTime.class, Types.TIME, (p, i, v) -> p.setTime(i, Time.valueOf(v)));
        register(LocalDateTime.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.valueOf(v)));
        register(OffsetDateTime.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.from(v.toInstant())));
        register(ZonedDateTime.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.from(v.toInstant())));
    }

    @Override
    public Optional<Function<Object, Argument>> prepare(QualifiedType<?> type, ConfigRegistry config) {
        return Optional.of(type).map(builders::get);
    }

    @Override
    public Optional<Argument> build(QualifiedType<?> type, Object value, ConfigRegistry config) {
        return prepare(type, config).map(f -> f.apply(value));
    }

    <T> void register(Class<T> klazz, int sqlType, StatementBinder<T> binder) {
        var type = QualifiedType.of(klazz).with(Legacy.class);
        builders.put(type,
            value -> value == null
                ? new NullArgument(sqlType)
                : new LoggableBinderArgument<>(klazz.cast(value), binder));
    }
}

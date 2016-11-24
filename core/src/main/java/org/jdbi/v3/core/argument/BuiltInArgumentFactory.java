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

import static org.jdbi.v3.core.util.GenericTypes.findGenericParameter;
import static org.jdbi.v3.core.util.GenericTypes.getErasedType;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.URI;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jdbi.v3.core.ConfigRegistry;
import org.jdbi.v3.core.SqlStatement;
import org.jdbi.v3.core.StatementContext;

/**
 * The BuiltInArgumentFactory provides instances of {@link Argument} for
 * many core Java types.  Generally you should not need to use this
 * class directly, but instead should bind your object with the
 * {@link SqlStatement} convenience methods.
 */
public class BuiltInArgumentFactory implements ArgumentFactory {
    // Care for the initialization order here, there's a fair number of statics.  Create the builders before the factory instance.

    private static final Argument<String> STR_BUILDER = new BuiltInArgument<>(String.class, Types.VARCHAR, PreparedStatement::setString);
    private static final Map<Class<?>, Argument<?>> CACHE = createInternalBuilders();

    public static final ArgumentFactory INSTANCE = new BuiltInArgumentFactory();

    private static <T> void register(Map<Class<?>, Argument<?>> map, Class<T> klass, int type, StatementBinder<T> binder) {
        register(map, klass, new BuiltInArgument<>(klass, type, binder));
    }

    private static <T> void register(Map<Class<?>, Argument<?>> map, Class<T> klass, Argument<T> argument) {
        map.put(klass, argument);
    }

    /** Create a binder which calls String.valueOf on its argument and then delegates to another binder. */
    private static <T> StatementBinder<T> stringifyValue(StatementBinder<String> real) {
        return (p, i, v) -> real.bind(p, i, String.valueOf(v));
    }

    private static Map<Class<?>, Argument<?>> createInternalBuilders() {
        final Map<Class<?>, Argument<?>> map = new IdentityHashMap<>();
        register(map, BigDecimal.class, Types.NUMERIC, PreparedStatement::setBigDecimal);
        register(map, Blob.class, Types.BLOB, PreparedStatement::setBlob);
        register(map, Boolean.class, Types.BOOLEAN, PreparedStatement::setBoolean);
        register(map, boolean.class, Types.BOOLEAN, PreparedStatement::setBoolean);
        register(map, Byte.class, Types.TINYINT, PreparedStatement::setByte);
        register(map, byte.class, Types.TINYINT, PreparedStatement::setByte);
        register(map, byte[].class, Types.VARBINARY, PreparedStatement::setBytes);
        register(map, Character.class, Types.CHAR, stringifyValue(PreparedStatement::setString));
        register(map, char.class, Types.CHAR, stringifyValue(PreparedStatement::setString));
        register(map, Clob.class, Types.CLOB, PreparedStatement::setClob);
        register(map, Double.class, Types.DOUBLE, PreparedStatement::setDouble);
        register(map, double.class, Types.DOUBLE, PreparedStatement::setDouble);
        register(map, Float.class, Types.FLOAT, PreparedStatement::setFloat);
        register(map, float.class, Types.FLOAT, PreparedStatement::setFloat);
        register(map, Inet4Address.class, Types.OTHER, (p, i, v) -> p.setString(i, v.getHostAddress()));
        register(map, Inet6Address.class, Types.OTHER, (p, i, v) -> p.setString(i, v.getHostAddress()));
        register(map, Integer.class, Types.INTEGER, PreparedStatement::setInt);
        register(map, int.class, Types.INTEGER, PreparedStatement::setInt);
        register(map, java.util.Date.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, new Timestamp(v.getTime())));
        register(map, Long.class, Types.INTEGER, PreparedStatement::setLong);
        register(map, long.class, Types.INTEGER, PreparedStatement::setLong);
        register(map, Short.class, Types.SMALLINT, PreparedStatement::setShort);
        register(map, short.class, Types.SMALLINT, PreparedStatement::setShort);
        register(map, java.sql.Date.class, Types.DATE, PreparedStatement::setDate);
        register(map, String.class, STR_BUILDER);
        register(map, Time.class, Types.TIME, PreparedStatement::setTime);
        register(map, Timestamp.class, Types.TIMESTAMP, PreparedStatement::setTimestamp);
        register(map, URL.class, Types.DATALINK, PreparedStatement::setURL);
        register(map, URI.class, Types.VARCHAR, stringifyValue(PreparedStatement::setString));
        register(map, UUID.class, Types.VARCHAR, PreparedStatement::setObject);

        register(map, Instant.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.from(v)));
        register(map, LocalDate.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.valueOf(v.atStartOfDay())));
        register(map, LocalDateTime.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.valueOf(v)));
        register(map, OffsetDateTime.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.from(v.toInstant())));
        register(map, ZonedDateTime.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.from(v.toInstant())));
        register(map, LocalTime.class, Types.TIME, (p, i, v) -> p.setTime(i, Time.valueOf(v)));

        register(map, AsciiStream.class, Types.LONGVARCHAR, (p, i, v) -> p.setAsciiStream(i, v.getStream(), v.getLength()));
        register(map, BinaryStream.class, Types.LONGVARBINARY, (p, i, v) -> p.setBinaryStream(i, v.getStream(), v.getLength()));
        register(map, CharacterStream.class, Types.LONGVARBINARY, (p, i, v) -> p.setCharacterStream(i, v.getReader(), v.getLength()));

        register(map, NullValue.class, (p, i, v, ctx) -> p.setNull(i, v.getSqlType()));

        return Collections.unmodifiableMap(map);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Argument<?>> build(Type expectedType, ConfigRegistry config)
    {
        Class<?> expectedClass = getErasedType(expectedType);

        @SuppressWarnings("rawtypes")
        Argument<?> a = CACHE.get(expectedClass);

        if (a != null) {
            return Optional.of(a);
        }

        // Enums must be bound as VARCHAR.
        if (expectedClass.isEnum()) {
            return Optional.of(new BuiltInArgument(
                    expectedClass,
                    Types.VARCHAR,
                    (p, i, v) -> p.setString(i, Enum.class.cast(v).name())));
        }

        if (Optional.class.equals(expectedClass)) {
            Optional<Argument<?>> argument = findGenericParameter(expectedType, Optional.class)
                    .flatMap(config::findArgumentFor)
                    .map(OptionalArgument::new);

            return argument.isPresent()
                    ? argument
                    : Optional.of(new BestEffortOptionalArgument());
        }

        return Optional.empty();
    }

    private static class OptionalArgument<T> implements Argument<Optional<T>> {
        private final Argument<T> delegate;

        OptionalArgument(Argument<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void apply(PreparedStatement statement, int position, Optional<T> value, StatementContext ctx) throws SQLException {
            delegate.apply(statement, position, value.orElse(null), ctx);
        }
    }

    @SuppressWarnings("unchecked")
    private static class BestEffortOptionalArgument implements Argument<Optional> {
        @Override
        public void apply(PreparedStatement statement, int position, Optional value, StatementContext ctx) throws SQLException {
            // no static type information -- best effort based on runtime type information
            Object nestedValue = value.orElseGet(NullValue::new);

            Argument<Object> argument = (Argument<Object>) ctx.findArgumentFor(nestedValue.getClass())
                    .orElseThrow(() -> new UnsupportedOperationException(
                            "No argument registered for value " + nestedValue + " of type " + nestedValue.getClass()));
            argument.apply(statement, position, nestedValue, ctx);
        }
    }

    @FunctionalInterface
    interface StatementBinder<T> {
        void bind(PreparedStatement p, int index, T value) throws SQLException;
    }

    static final class BuiltInArgument<T> implements Argument<T> {
        private final int type;
        private final StatementBinder<T> binder;

        private BuiltInArgument(Class<T> klass, int type, StatementBinder<T> binder) {
            this.binder = binder;
            this.type = type;
        }

        @Override
        public void apply(PreparedStatement statement, int position, T value, StatementContext ctx) throws SQLException {
            if (value == null) {
                statement.setNull(position, type);
                return;
            }
            binder.bind(statement, position, value);
        }

        @Override
        public String toString() {
            return "BuiltInArgument<" + type + ">";
        }
    }
}

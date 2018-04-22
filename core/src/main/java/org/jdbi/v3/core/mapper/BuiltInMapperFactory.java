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
package org.jdbi.v3.core.mapper;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Column mapper factory which knows how to map JDBC-recognized types, along with some other well-known types
 * from the JDK.
 */
public class BuiltInMapperFactory implements ColumnMapperFactory {
    private static final Map<Class<?>, ColumnMapper<?>> MAPPERS = new HashMap<>();

    static {
        MAPPERS.put(boolean.class, primitiveMapper(ResultSet::getBoolean));
        MAPPERS.put(byte.class, primitiveMapper(ResultSet::getByte));
        MAPPERS.put(char.class, primitiveMapper(BuiltInMapperFactory::getChar));
        MAPPERS.put(short.class, primitiveMapper(ResultSet::getShort));
        MAPPERS.put(int.class, primitiveMapper(ResultSet::getInt));
        MAPPERS.put(long.class, primitiveMapper(ResultSet::getLong));
        MAPPERS.put(float.class, primitiveMapper(ResultSet::getFloat));
        MAPPERS.put(double.class, primitiveMapper(ResultSet::getDouble));

        MAPPERS.put(Boolean.class, referenceMapper(ResultSet::getBoolean));
        MAPPERS.put(Byte.class, referenceMapper(ResultSet::getByte));
        MAPPERS.put(Character.class, referenceMapper(BuiltInMapperFactory::getCharacter));
        MAPPERS.put(Short.class, referenceMapper(ResultSet::getShort));
        MAPPERS.put(Integer.class, referenceMapper(ResultSet::getInt));
        MAPPERS.put(Long.class, referenceMapper(ResultSet::getLong));
        MAPPERS.put(Float.class, referenceMapper(ResultSet::getFloat));
        MAPPERS.put(Double.class, referenceMapper(ResultSet::getDouble));

        MAPPERS.put(BigDecimal.class, referenceMapper(ResultSet::getBigDecimal));

        MAPPERS.put(String.class, referenceMapper(ResultSet::getString));

        MAPPERS.put(byte[].class, referenceMapper(ResultSet::getBytes));

        MAPPERS.put(Timestamp.class, referenceMapper(ResultSet::getTimestamp));

        MAPPERS.put(InetAddress.class, BuiltInMapperFactory::getInetAddress);

        MAPPERS.put(URL.class, referenceMapper(ResultSet::getURL));
        MAPPERS.put(URI.class, referenceMapper(BuiltInMapperFactory::getURI));
        MAPPERS.put(UUID.class, BuiltInMapperFactory::getUUID);

        MAPPERS.put(Instant.class, referenceMapper(BuiltInMapperFactory::getInstant));
        MAPPERS.put(LocalDate.class, referenceMapper(BuiltInMapperFactory::getLocalDate));
        MAPPERS.put(LocalDateTime.class, referenceMapper(BuiltInMapperFactory::getLocalDateTime));
        MAPPERS.put(OffsetDateTime.class, referenceMapper(BuiltInMapperFactory::getOffsetDateTime));
        MAPPERS.put(ZonedDateTime.class, referenceMapper(BuiltInMapperFactory::getZonedDateTime));
        MAPPERS.put(LocalTime.class, referenceMapper(BuiltInMapperFactory::getLocalTime));
        MAPPERS.put(ZoneId.class, referenceMapper(BuiltInMapperFactory::getZoneId));

        MAPPERS.put(OptionalInt.class, optionalMapper(ResultSet::getInt, OptionalInt::empty, OptionalInt::of));
        MAPPERS.put(OptionalLong.class, optionalMapper(ResultSet::getLong, OptionalLong::empty, OptionalLong::of));
        MAPPERS.put(OptionalDouble.class, optionalMapper(ResultSet::getDouble, OptionalDouble::empty, OptionalDouble::of));
    }

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        Class<?> rawType = getErasedType(type);
        if (rawType.isEnum()) {
            return Optional.of(EnumMapper.byName(rawType.asSubclass(Enum.class)));
        }
        if (rawType == Optional.class) {
            return Optional.of(OptionalMapper.of(type));
        }

        return Optional.ofNullable(MAPPERS.get(rawType));
    }

    @FunctionalInterface
    interface ColumnGetter<T> {
        T get(ResultSet rs, int i) throws SQLException;
    }

    private static <T> ColumnMapper<T> primitiveMapper(ColumnGetter<T> getter) {
        return (r, i, ctx) -> getter.get(r, i);
    }

    private static <T> ColumnMapper<T> referenceMapper(ColumnGetter<T> getter) {
        return (r, i, ctx) -> {
            T value = getter.get(r, i);
            return r.wasNull() ? null : value;
        };
    }

    private static char getChar(ResultSet r, int i) throws SQLException {
        Character character = getCharacter(r, i);
        return character == null ? '\000' : character;
    }

    private static Character getCharacter(ResultSet r, int i) throws SQLException {
        String s = r.getString(i);
        if (s != null && !s.isEmpty()) {
            return s.charAt(0);
        }
        return null;
    }

    private static URI getURI(ResultSet r, int i) throws SQLException {
        String s = r.getString(i);
        try {
            return (s != null) ? (new URI(s)) : null;
        } catch (URISyntaxException e) {
            throw new SQLException("Failed to convert data to URI", e);
        }
    }

    private static UUID getUUID(ResultSet r, int i, StatementContext ctx) throws SQLException {
        String s = r.getString(i);
        if (s == null) {
            return null;
        }
        return UUID.fromString(s);
    }

    private static Instant getInstant(ResultSet r, int i) throws SQLException {
        Timestamp ts = r.getTimestamp(i);
        return ts == null ? null : ts.toInstant();
    }

    private static LocalDate getLocalDate(ResultSet r, int i) throws SQLException {
        Timestamp ts = r.getTimestamp(i);
        return ts == null ? null : ts.toLocalDateTime().toLocalDate();
    }

    private static LocalDateTime getLocalDateTime(ResultSet r, int i) throws SQLException {
        Timestamp ts = r.getTimestamp(i);
        return ts == null ? null : ts.toLocalDateTime();
    }

    private static OffsetDateTime getOffsetDateTime(ResultSet r, int i) throws SQLException {
        Timestamp ts = r.getTimestamp(i);
        return ts == null ? null : OffsetDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
    }

    private static ZonedDateTime getZonedDateTime(ResultSet r, int i) throws SQLException {
        Timestamp ts = r.getTimestamp(i);
        return ts == null ? null : ZonedDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
    }

    private static LocalTime getLocalTime(ResultSet r, int i) throws SQLException {
        Time time = r.getTime(i);
        return time == null ? null : time.toLocalTime();
    }

    private static ZoneId getZoneId(ResultSet r, int i) throws SQLException {
        String id = r.getString(i);
        return id == null ? null : ZoneId.of(id);
    }

    private static InetAddress getInetAddress(ResultSet r, int i, StatementContext ctx) throws SQLException {
        String hostname = r.getString(i);
        try {
            return hostname == null ? null : InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            throw new MappingException("Could not map InetAddress", e);
        }
    }

    private static <Opt, Box> ColumnMapper<?> optionalMapper(ColumnGetter<Box> columnGetter, Supplier<Opt> empty, Function<Box, Opt> present) {
        return new ColumnMapper<Opt>() {
            @Override
            public Opt map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
                final Box boxed = referenceMapper(columnGetter).map(r, columnNumber, ctx);
                if (boxed == null) {
                    return empty.get();
                }
                return present.apply(boxed);
            }
        };
    }
}

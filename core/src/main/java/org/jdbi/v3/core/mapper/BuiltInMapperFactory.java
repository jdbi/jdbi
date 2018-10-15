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
import java.util.function.Function;
import java.util.function.Supplier;
import org.jdbi.v3.core.config.ConfigRegistry;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Column mapper factory which knows how to map JDBC-recognized types, along with some other well-known types
 * from the JDK.
 */
public class BuiltInMapperFactory implements ColumnMapperFactory {
    private static final Map<Class<?>, ColumnMapper<?>> MAPPERS = new HashMap<>();

    static {
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

        return Optional.ofNullable(MAPPERS.get(rawType));
    }

    private static <T> ColumnMapper<T> referenceMapper(ColumnGetter<T> getter) {
        return (r, i, ctx) -> {
            T value = getter.get(r, i);
            return r.wasNull() ? null : value;
        };
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

    private static <Opt, Box> ColumnMapper<?> optionalMapper(ColumnGetter<Box> columnGetter, Supplier<Opt> empty, Function<Box, Opt> present) {
        return (ColumnMapper<Opt>) (r, columnNumber, ctx) -> {
            final Box boxed = referenceMapper(columnGetter).map(r, columnNumber, ctx);
            if (boxed == null) {
                return empty.get();
            }
            return present.apply(boxed);
        };
    }

}

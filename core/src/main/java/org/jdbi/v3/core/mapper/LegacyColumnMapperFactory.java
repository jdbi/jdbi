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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.meta.Legacy;

/**
 * Supports alternative mappings for types by using the {@link Legacy} qualifier.
 * <br>
 * Restores the non-{@link java.sql.ResultSet#getObject} behavior for java time types. This is a fallback when using {@link java.sql.ResultSet#getObject} is not
 * available or does not work with a JDBC driver.
 *
 * @since 3.52.0
 */
final class LegacyColumnMapperFactory implements QualifiedColumnMapperFactory {

    private static final Map<QualifiedType<?>, ColumnMapper<?>> MAPPERS;

    static {
        Map<QualifiedType<?>, ColumnMapper<?>> map = new HashMap<>();
        register(map, Instant.class, new GetterMapper<>(LegacyColumnMapperFactory::getLegacyInstant));
        register(map, LocalDate.class, new GetterMapper<>(LegacyColumnMapperFactory::getLegacyLocalDate));
        register(map, LocalTime.class, new GetterMapper<>(LegacyColumnMapperFactory::getLegacyLocalTime));
        register(map, LocalDateTime.class, new GetterMapper<>(LegacyColumnMapperFactory::getLegacyLocalDateTime));
        register(map, OffsetDateTime.class, new GetterMapper<>(LegacyColumnMapperFactory::getLegacyOffsetDateTime));
        register(map, ZonedDateTime.class, new GetterMapper<>(LegacyColumnMapperFactory::getLegacyZonedDateTime));
        register(map, OffsetTime.class, new GetterMapper<>(LegacyColumnMapperFactory::getLegacyOffsetTime));

        MAPPERS = map;
    }

    LegacyColumnMapperFactory() {}

    private static void register(Map<QualifiedType<?>, ColumnMapper<?>> map, Class<?> clazz, ColumnMapper<?> mapper) {
        QualifiedType<?> type = QualifiedType.of(clazz).with(Legacy.class);
        map.put(type, mapper);
    }

    @Override
    public Optional<ColumnMapper<?>> build(QualifiedType<?> type, ConfigRegistry config) {
        return Optional.of(type).map(MAPPERS::get);
    }

    private static Instant getLegacyInstant(ResultSet r, int i) throws SQLException {
        Timestamp ts = r.getTimestamp(i);
        return ts == null ? null : ts.toInstant();
    }

    private static LocalDate getLegacyLocalDate(ResultSet r, int i) throws SQLException {
        Timestamp ts = r.getTimestamp(i);
        return ts == null ? null : ts.toLocalDateTime().toLocalDate();
    }

    private static LocalDateTime getLegacyLocalDateTime(ResultSet r, int i) throws SQLException {
        Timestamp ts = r.getTimestamp(i);
        return ts == null ? null : ts.toLocalDateTime();
    }

    private static LocalTime getLegacyLocalTime(ResultSet r, int i) throws SQLException {
        Time time = r.getTime(i);
        return time == null ? null : time.toLocalTime();
    }

    private static OffsetDateTime getLegacyOffsetDateTime(ResultSet r, int i) throws SQLException {
        Timestamp ts = r.getTimestamp(i);
        return ts == null ? null : OffsetDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
    }

    private static ZonedDateTime getLegacyZonedDateTime(ResultSet r, int i) throws SQLException {
        Timestamp ts = r.getTimestamp(i);
        return ts == null ? null : ZonedDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
    }

    private static OffsetTime getLegacyOffsetTime(ResultSet r, int i) throws SQLException {
        var t = r.getTime(i);
        return (t == null) ? null : OffsetTime.of(t.toLocalTime(), ZoneOffset.UTC);
    }
}

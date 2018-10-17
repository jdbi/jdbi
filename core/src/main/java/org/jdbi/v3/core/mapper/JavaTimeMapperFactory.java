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
import org.jdbi.v3.core.config.ConfigRegistry;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Column mapper factory which knows how to map JavaTime objects:
 * <ul>
 *     <li>{@link Instant}</li>
 *     <li>{@link LocalDate}</li>
 *     <li>{@link LocalTime}</li>
 *     <li>{@link LocalDateTime}</li>
 *     <li>{@link OffsetDateTime}</li>
 *     <li>{@link ZonedDateTime}</li>
 *     <li>{@link ZoneId}</li>
 * </ul>
 */
class JavaTimeMapperFactory implements ColumnMapperFactory {
    private static final Map<Class<?>, ColumnMapper<?>> MAPPERS = new HashMap<>();

    static {
        MAPPERS.put(Instant.class, new ReferenceMapper<>(JavaTimeMapperFactory::getInstant));
        MAPPERS.put(LocalDate.class, new ReferenceMapper<>(JavaTimeMapperFactory::getLocalDate));
        MAPPERS.put(LocalTime.class, new ReferenceMapper<>(JavaTimeMapperFactory::getLocalTime));
        MAPPERS.put(LocalDateTime.class, new ReferenceMapper<>(JavaTimeMapperFactory::getLocalDateTime));
        MAPPERS.put(OffsetDateTime.class, new ReferenceMapper<>(JavaTimeMapperFactory::getOffsetDateTime));
        MAPPERS.put(ZonedDateTime.class, new ReferenceMapper<>(JavaTimeMapperFactory::getZonedDateTime));
        MAPPERS.put(ZoneId.class, new ReferenceMapper<>(JavaTimeMapperFactory::getZoneId));
    }

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        Class<?> rawType = getErasedType(type);

        return Optional.ofNullable(MAPPERS.get(rawType));
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
        // TODO systemDefault(), eww, provide config instead
        return ts == null ? null : OffsetDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
    }

    private static ZonedDateTime getZonedDateTime(ResultSet r, int i) throws SQLException {
        Timestamp ts = r.getTimestamp(i);
        // TODO systemDefault(), eww, provide config instead
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
}

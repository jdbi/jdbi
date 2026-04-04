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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;

/**
 * Column mapper factory which knows how to map JavaTime objects:
 * <ul>
 *     <li>{@link Instant}</li>
 *     <li>{@link LocalDate}</li>
 *     <li>{@link LocalTime}</li>
 *     <li>{@link LocalDateTime}</li>
 *     <li>{@link OffsetDateTime}</li>
 *     <li>{@link OffsetTime}</li>
 *     <li>{@link ZonedDateTime}</li>
 *     <li>{@link ZoneId}</li>
 * </ul>
 */
class JavaTimeMapperFactory extends GetObjectColumnMapperFactory {

    private static final Map<Class<?>, ColumnMapper<?>> MAPPERS = Map.of(
        ZoneId.class, (r, i, ctx) -> getZoneId(r, i),
        ZonedDateTime.class, (r, i, ctx) -> getZonedDateTime(r, i)
    );

    JavaTimeMapperFactory() {
        super(Instant.class, LocalDate.class, LocalTime.class, LocalDateTime.class, OffsetDateTime.class, OffsetTime.class);
    }

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {

        Optional<ColumnMapper<?>> res = Optional.of(type)
            .filter(Class.class::isInstance)
            .map(Class.class::cast)
            .map(MAPPERS::get);

        if (res.isEmpty()) {
            res = super.build(type, config);
        }

        return res;
    }

    private static ZoneId getZoneId(ResultSet rs, int columnNumber) throws SQLException {
        var zoneId = rs.getString(columnNumber);
        return zoneId == null ? null : ZoneId.of(zoneId);
    }

    private static ZonedDateTime getZonedDateTime(ResultSet rs, int columnNumber) throws SQLException {
        var offsetDateTime = rs.getObject(columnNumber, OffsetDateTime.class);
        return offsetDateTime == null ? null : offsetDateTime.toZonedDateTime();
    }
}

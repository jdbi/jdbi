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
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.meta.Legacy;

/**
 * Provides alternative column mapping strategies for legacy types.
 * <p>
 * Currently supports {@link OffsetDateTime} and {@link ZonedDateTime}.
 */
public class LegacyColumnMapperFactory implements QualifiedColumnMapperFactory {

    private final ConcurrentMap<QualifiedType<?>, ColumnMapper<?>> mappers = new ConcurrentHashMap<>();

    LegacyColumnMapperFactory() {
        mappers.put(QualifiedType.of(OffsetDateTime.class).with(Legacy.class), new GetterMapper<>(LegacyColumnMapperFactory::getLegacyOffsetDateTime));
        mappers.put(QualifiedType.of(ZonedDateTime.class).with(Legacy.class), new GetterMapper<>(LegacyColumnMapperFactory::getLegacyZonedDateTime));
    }

    @Override
    public Optional<ColumnMapper<?>> build(QualifiedType<?> type, ConfigRegistry config) {
        return Optional.of(type).map(mappers::get);
    }

    private static OffsetDateTime getLegacyOffsetDateTime(ResultSet r, int i) throws SQLException {
        Timestamp ts = r.getTimestamp(i);
        return ts == null ? null : OffsetDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
    }

    private static ZonedDateTime getLegacyZonedDateTime(ResultSet r, int i) throws SQLException {
        Timestamp ts = r.getTimestamp(i);
        return ts == null ? null : ZonedDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
    }
}

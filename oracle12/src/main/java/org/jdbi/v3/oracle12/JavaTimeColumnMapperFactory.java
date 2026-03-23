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
package org.jdbi.v3.oracle12;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;

/**
 * Oracle specific mapper factory for java time types.
 *
 * {@link Instant} must be explicitly mapped from {@link Timestamp} as the Oracle driver does not support it with {@link ResultSet#getObject}.
 */
final class JavaTimeColumnMapperFactory implements ColumnMapperFactory {

    private static final Map<Class<?>, ColumnMapper<?>> MAPPERS = Map.of(
        Instant.class, (r, columnNumber, ctx) -> getOracleInstant(r, columnNumber)
    );

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        return Optional.of(type)
            .filter(Class.class::isInstance)
            .map(Class.class::cast)
            .map(MAPPERS::get);
    }

    private static Instant getOracleInstant(ResultSet r, int i) throws SQLException {
        Timestamp ts = r.getTimestamp(i);
        return ts == null ? null : ts.toInstant();
    }
}

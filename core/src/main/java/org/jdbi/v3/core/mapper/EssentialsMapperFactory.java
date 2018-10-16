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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Column mapper factory which knows how to map high-level essentials like String:
 * <ul>
 *     <li>{@link BigDecimal}</li>
 *     <li>{@link String}</li>
 *     <li>{@code byte[]}</li>
 *     <li>{@link UUID}</li>
 * </ul>
 */
class EssentialsMapperFactory implements ColumnMapperFactory {
    private static final Map<Class<?>, ColumnMapper<?>> MAPPERS = new HashMap<>();

    // TODO consider other types?

    static {
        MAPPERS.put(BigDecimal.class, new ReferenceMapper<>(ResultSet::getBigDecimal));
        MAPPERS.put(String.class, new ReferenceMapper<>(ResultSet::getString));
        MAPPERS.put(byte[].class, new ReferenceMapper<>(ResultSet::getBytes));
        MAPPERS.put(UUID.class, EssentialsMapperFactory::getUUID);
    }

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        Class<?> rawType = getErasedType(type);

        return Optional.ofNullable(MAPPERS.get(rawType));
    }

    private static UUID getUUID(ResultSet r, int i, StatementContext ctx) throws SQLException {
        String s = r.getString(i);

        return s == null ? null : UUID.fromString(s);
    }
}

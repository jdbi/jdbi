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
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.statement.StatementContext;

class EnumByNameMapper<E extends Enum<E>> implements ColumnMapper<E> {
    private final Class<E> type;
    private final ConcurrentMap<String, E> lookups = new ConcurrentHashMap<>();

    EnumByNameMapper(Class<E> type) {
        this.type = type;
    }

    @Override
    public E map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
        String name = ctx.findColumnMapperFor(String.class)
            .orElseThrow(() -> new UnableToProduceResultException("a String column mapper is required to map Enums from names", ctx))
            .map(rs, columnNumber, ctx);

        return name == null ? null : lookups.computeIfAbsent(name.toLowerCase(), lowercased -> matchByName(name, ctx));
    }

    private E matchByName(String name, StatementContext ctx) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException ignored) {
            return Arrays.stream(type.getEnumConstants())
                .filter(e -> e.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new UnableToProduceResultException(String.format(
                    "no %s value could be matched to the name %s", type.getSimpleName(), name
                ), ctx));
        }
    }
}

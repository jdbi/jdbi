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
package org.jdbi.v3.core.internal.enums;

import java.sql.Types;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jdbi.v3.core.EnumByName;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.statement.StatementContext;

@EnumByName
public class ByName implements EnumStrategy {
    private static final EnumStrategy INSTANCE = new ByName();

    public static EnumStrategy singleton() {
        return INSTANCE;
    }

    @Override
    public int nullType() {
        return Types.VARCHAR;
    }

    @Override
    public Optional<Argument> bind(Arguments args, Enum<?> value) {
        return args.findFor(String.class, value.name());
    }

    @Override
    public <E extends Enum<E>> ColumnMapper<E> getMapper(Class<E> enumClass) {
        final ConcurrentMap<String, Enum<?>> cache = new ConcurrentHashMap<>();
        return (rs, col, ctx) -> {
            String name = ctx.findColumnMapperFor(String.class)
                    .orElseThrow(() -> new UnableToProduceResultException("a String column mapper is required to map Enums from names", ctx))
                    .map(rs, col, ctx);

            return name == null ? null : enumClass.cast(cache.computeIfAbsent(name.toLowerCase(), lowercased -> matchByName(enumClass, name, ctx)));
        };
    }

    private static <E extends Enum<E>> E matchByName(Class<E> type, String name, StatementContext ctx) {
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

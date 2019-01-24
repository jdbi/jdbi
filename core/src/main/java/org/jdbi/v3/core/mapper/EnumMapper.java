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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.jdbi.v3.core.enums.EnumByName;
import org.jdbi.v3.core.enums.EnumByOrdinal;
import org.jdbi.v3.core.enums.Enums;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Column mapper for Java {@code enum} types.
 * @param <E> the enum type mapped
 *
 * @see Enums
 * @see EnumByName
 * @see EnumByOrdinal
 * @see EnumMapperFactory
 */
public abstract class EnumMapper<E extends Enum<E>> implements ColumnMapper<E> {
    private static final Map<Class<? extends Enum<?>>, ColumnMapper<? extends Enum<?>>> BY_NAME_MAPPER_CACHE =
        ExpiringMap.builder().expiration(10, TimeUnit.MINUTES).expirationPolicy(ExpirationPolicy.ACCESSED).build();

    EnumMapper() {}

    /**
     * @param <E> the enum type to map
     * @param type the enum type to map
     * @return an enum mapper that matches on {@link Enum#name()}
     */
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> ColumnMapper<E> byName(Class<E> type) {
        return (ColumnMapper<E>) BY_NAME_MAPPER_CACHE.computeIfAbsent(type, t -> new EnumByNameColumnMapper<>(type));
    }

    /**
     * @param <E> the enum type to map
     * @param type the enum type to map
     * @return an enum mapper that matches on {@link Enum#ordinal()}
     */
    public static <E extends Enum<E>> ColumnMapper<E> byOrdinal(Class<E> type) {
        return new EnumByOrdinalColumnMapper<>(type);
    }

    static class EnumByNameColumnMapper<E extends Enum<E>> implements ColumnMapper<E> {
        private final Map<String, E> nameValueCache = new ConcurrentHashMap<>();
        private final Class<E> enumClass;

        private EnumByNameColumnMapper(Class<E> enumClass) {
            this.enumClass = enumClass;
        }

        @Override
        public E map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
            String name = ctx.findColumnMapperFor(String.class)
                .orElseThrow(() -> new UnableToProduceResultException("a String column mapper is required to map Enums from names", ctx))
                .map(rs, columnNumber, ctx);

            return name == null ? null : nameValueCache.computeIfAbsent(name.toLowerCase(), lowercased -> getValueByName(enumClass, name, ctx));
        }

        private static <E extends Enum<E>> E getValueByName(Class<E> enumClass, String name, StatementContext ctx) {
            try {
                return Enum.valueOf(enumClass, name);
            } catch (IllegalArgumentException ignored) {
                return Arrays.stream(enumClass.getEnumConstants())
                    .filter(e -> e.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new UnableToProduceResultException(String.format(
                        "no %s value could be matched to the name %s", enumClass.getSimpleName(), name
                    ), ctx));
            }
        }
    }

    static class EnumByOrdinalColumnMapper<E extends Enum<E>> implements ColumnMapper<E> {
        private final Class<E> enumClass;

        private EnumByOrdinalColumnMapper(Class<E> enumClass) {
            this.enumClass = enumClass;
        }

        @Override
        public E map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
            Integer ordinal = ctx.findColumnMapperFor(Integer.class)
                .orElseThrow(() -> new UnableToProduceResultException("an Integer column mapper is required to map Enums from ordinals", ctx))
                .map(rs, columnNumber, ctx);

            return ordinal == null ? null : getValueByOrdinal(enumClass, ordinal, ctx);
        }

        private static <E extends Enum<E>> E getValueByOrdinal(Class<E> enumClass, int ordinal, StatementContext ctx) {
            try {
                return enumClass.getEnumConstants()[ordinal];
            } catch (ArrayIndexOutOfBoundsException oob) {
                throw new UnableToProduceResultException(String.format(
                    "no %s value could be matched to the ordinal %s", enumClass.getSimpleName(), ordinal
                ), oob, ctx);
            }
        }
    }
}

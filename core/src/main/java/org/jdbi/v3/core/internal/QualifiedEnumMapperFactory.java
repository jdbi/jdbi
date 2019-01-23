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
package org.jdbi.v3.core.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbi.v3.core.Enums;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.QualifiedColumnMapperFactory;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.statement.StatementContext;

public class QualifiedEnumMapperFactory implements QualifiedColumnMapperFactory {
    private static final Map<Class<? extends Enum<?>>, ColumnMapper<? extends Enum<?>>> NAME_MAPPER_CACHE = new ConcurrentHashMap<>();

    @Override
    public Optional<ColumnMapper<?>> build(QualifiedType<?> givenType, ConfigRegistry config) {
        return Optional.of(givenType.getType())
            .map(GenericTypes::getErasedType)
            .filter(Class::isEnum)
            .flatMap(clazz -> makeEnumArgument((QualifiedType<Enum>) givenType, (Class<Enum>) clazz, config));
    }

    private static <E extends Enum<E>> Optional<ColumnMapper<?>> makeEnumArgument(QualifiedType<E> givenType, Class<E> enumClass, ConfigRegistry config) {
        boolean byName = Enums.EnumStrategy.BY_NAME == config.get(Enums.class).findStrategy(givenType, enumClass);

        return Optional.of(byName
            ? byName(enumClass)
            : byOrdinal(enumClass));
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> ColumnMapper<E> byName(Class<E> enumClass) {
        return (ColumnMapper<E>) NAME_MAPPER_CACHE.computeIfAbsent(enumClass, e -> new EnumByNameColumnMapper<>(enumClass));
    }

    public static <E extends Enum<E>> ColumnMapper<E> byOrdinal(Class<E> enumClass) {
        return new EnumByOrdinalColumnMapper<>(enumClass);
    }

    private static class EnumByNameColumnMapper<E extends Enum<E>> implements ColumnMapper<E> {
        private final Map<String, E> NAME_CACHE = new ConcurrentHashMap<>();
        private final Class<E> enumClass;

        private EnumByNameColumnMapper(Class<E> enumClass) {
            this.enumClass = enumClass;
        }

        @Override
        public E map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
            String name = ctx.findColumnMapperFor(String.class)
                .orElseThrow(() -> new UnableToProduceResultException("a String column mapper is required to map Enums from names", ctx))
                .map(rs, columnNumber, ctx);

            return name == null ? null : NAME_CACHE.computeIfAbsent(name.toLowerCase(), lowercased -> getValueByName(enumClass, name, ctx));
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

    private static class EnumByOrdinalColumnMapper<E extends Enum<E>> implements ColumnMapper<E> {
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

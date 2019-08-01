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
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jdbi.v3.core.config.JdbiCache;
import org.jdbi.v3.core.config.JdbiCaches;
import org.jdbi.v3.core.enums.DatabaseValue;
import org.jdbi.v3.core.enums.EnumByName;
import org.jdbi.v3.core.enums.EnumByOrdinal;
import org.jdbi.v3.core.enums.Enums;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Column mapper for Java {@code enum} types.
 * @param <E> the enum type mapped
 *
 * @see Enums
 * @see EnumByName
 * @see EnumByOrdinal
 */
// TODO jdbi4: move to enums package
public abstract class EnumMapper<E extends Enum<E>> implements ColumnMapper<E> {
    EnumMapper() {}

    /**
     * @param <E> the enum type to map
     * @param type the enum type to map
     * @return an enum mapper that matches on {@link Enum#name()}
     */
    public static <E extends Enum<E>> ColumnMapper<E> byName(Class<E> type) {
        return new EnumByNameColumnMapper<>(type);
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
        private static final JdbiCache<Class<? extends Enum<?>>, JdbiCache<String, Enum<?>>> BY_NAME_CACHE =
                JdbiCaches.declare(e -> JdbiCaches.declare(
                        name -> e.cast(getValueByName(e, name))));
        private final Class<E> enumClass;

        private EnumByNameColumnMapper(Class<E> enumClass) {
            this.enumClass = enumClass;
        }

        @Override
        public E map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
            String name = rs.getString(columnNumber);

            return name == null || name.isEmpty() // some vendors treat null and empty varchar as the same
                ? null
                : enumClass.cast(BY_NAME_CACHE.get(enumClass, ctx).get(name, ctx));
        }

        private static Object getValueByName(Class<? extends Enum<?>> enumClass, String name) {
            return Stream.concat(
                        enumConstants(enumClass).filter(enumValueMatches(name, String::equals)),
                        enumConstants(enumClass).filter(enumValueMatches(name, String::equalsIgnoreCase))
                    )
                    .findFirst()
                    .orElseThrow(() -> new UnableToProduceResultException(
                        String.format("no %s value could be matched to the name %s", enumClass.getSimpleName(), name)));
        }

        private static Predicate<Enum<?>> enumValueMatches(String name, BiPredicate<String, String> comparer) {
            return e -> Optional.ofNullable(
                Unchecked.function(e.getClass()::getField)
                    .apply(e.name())
                    .getAnnotation(DatabaseValue.class))
                .map(DatabaseValue::value)
                .map(dbName -> comparer.test(dbName, name))
                .orElseGet(() -> comparer.test(e.name(), name));
        }

        private static Stream<Enum<?>> enumConstants(Class<? extends Enum<?>> enumClass) {
            return Arrays.stream(enumClass.getEnumConstants());
        }
    }

    static class EnumByOrdinalColumnMapper<E extends Enum<E>> implements ColumnMapper<E> {
        private final Class<E> enumClass;
        private final E[] enumConstants;

        private EnumByOrdinalColumnMapper(Class<E> enumClass) {
            this.enumClass = enumClass;
            this.enumConstants = enumClass.getEnumConstants();
        }

        @Override
        public E map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
            int ordinal = rs.getInt(columnNumber);

            try {
                return rs.wasNull() ? null : enumConstants[ordinal];
            } catch (ArrayIndexOutOfBoundsException oob) {
                throw new UnableToProduceResultException(String.format(
                        "no %s value could be matched to the ordinal %s", enumClass.getSimpleName(), ordinal),
                    oob, ctx);
            }
        }
    }
}

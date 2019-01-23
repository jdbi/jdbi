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

import org.jdbi.v3.core.internal.QualifiedEnumMapperFactory;

/**
 * Column mapper for Java {@code enum} types.
 * @param <E> the enum type mapped
 *
 * @deprecated this class has been superseded by a new implementation
 * @see org.jdbi.v3.core.Enums
 * @see org.jdbi.v3.core.EnumByName
 * @see org.jdbi.v3.core.EnumByOrdinal
 * @see QualifiedEnumMapperFactory
 */
@Deprecated
// TODO jdbi4: delete
public abstract class EnumMapper<E extends Enum<E>> implements ColumnMapper<E> {
    EnumMapper() {}

    /**
     * @param <E> the enum type to map
     * @param type the enum type to map
     * @return an enum mapper that matches on {@link Enum#name()}
     * @see QualifiedEnumMapperFactory#byName(Class)
     * @deprecated superseded by new implementation
     */
    @Deprecated
    public static <E extends Enum<E>> ColumnMapper<E> byName(Class<E> type) {
        return QualifiedEnumMapperFactory.byName(type);
    }

    /**
     * @param <E> the enum type to map
     * @param type the enum type to map
     * @return an enum mapper that matches on {@link Enum#ordinal()}
     * @see QualifiedEnumMapperFactory#byOrdinal(Class)
     * @deprecated superseded by new implementation
     */
    @Deprecated
    public static <E extends Enum<E>> ColumnMapper<E> byOrdinal(Class<E> type) {
        return QualifiedEnumMapperFactory.byOrdinal(type);
    }
}

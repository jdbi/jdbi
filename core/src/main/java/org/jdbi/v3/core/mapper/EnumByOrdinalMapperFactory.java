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
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.internal.QualifiedEnumMapperFactory;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Produces enum column mappers, which map enums from numeric columns according to ordinal value.
 *
 * @deprecated this class has been superseded by a new implementation
 * @see org.jdbi.v3.core.Enums
 * @see org.jdbi.v3.core.EnumByName
 * @see org.jdbi.v3.core.EnumByOrdinal
 */
@Deprecated
// TODO jdbi4: delete
public class EnumByOrdinalMapperFactory implements ColumnMapperFactory {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        Class<?> clazz = getErasedType(type);
        return clazz.isEnum()
                ? Optional.of(new QualifiedEnumMapperFactory.EnumByOrdinalColumnMapper<>(clazz.asSubclass(Enum.class)))
                : Optional.empty();
    }
}

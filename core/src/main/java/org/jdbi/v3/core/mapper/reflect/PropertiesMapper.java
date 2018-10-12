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
package org.jdbi.v3.core.mapper.reflect;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoProperty;

class PropertiesMapper<T> extends BeanMapper<T> {
    private PropertiesMapper(Class<T> type, PojoProperties<T> properties, String prefix) {
        super(type, properties, prefix);
    }

    public static <T> RowMapperFactory factory(Class<T> type, PojoProperties<T> properties) {
        return RowMapperFactory.of(type, PropertiesMapper.of(type, properties));
    }

    public static <T> RowMapperFactory factory(Class<T> type, PojoProperties<T> properties, String prefix) {
        return RowMapperFactory.of(type, PropertiesMapper.of(type, properties, prefix));
    }

    public static <T> RowMapper<T> of(Class<T> type, PojoProperties<T> properties) {
        return PropertiesMapper.of(type, properties, DEFAULT_PREFIX);
    }

    public static <T> RowMapper<T> of(Class<T> type, PojoProperties<T> properties, String prefix) {
        return new PropertiesMapper<>(type, properties, prefix);
    }

    @Override
    protected ColumnMapper<?> defaultColumnMapper(PojoProperty<T> property) {
        throw new NoSuchMapperException(String.format(
                "Couldn't find mapper for property '%s' of type '%s' from %s", property.getName(), property.getQualifiedType(), type));
    }
}

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

import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.reflect.internal.BeanPropertiesFactory;
import org.jdbi.v3.core.mapper.reflect.internal.PojoMapper;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoProperty;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * A row mapper which maps the columns in a statement into a JavaBean. The default
 * implementation will perform a case insensitive mapping between the bean property
 * names and the column labels, also considering camel-case to underscores conversion.
 * This uses the JDK's built in bean mapping facilities, so it does not support nested
 * properties.
 *
 * The mapped class must have a default constructor.
 *
 * @deprecated this class should not be public API, use {@link org.jdbi.v3.core.result.ResultBearing#mapToBean(Class)} instead.
 */
@Deprecated
public class BeanMapper<T> extends PojoMapper<T> {
    static final String DEFAULT_PREFIX = "";
    /**
     * Returns a mapper factory that maps to the given bean class
     *
     * @param type the mapped class
     * @return a mapper factory that maps to the given bean class
     */
    public static RowMapperFactory factory(Class<?> type) {
        return RowMapperFactory.of(type, BeanMapper.of(type));
    }

    /**
     * Returns a mapper factory that maps to the given bean class
     *
     * @param type the mapped class
     * @param prefix the column name prefix for each mapped bean property
     * @return a mapper factory that maps to the given bean class
     */
    public static RowMapperFactory factory(Class<?> type, String prefix) {
        return RowMapperFactory.of(type, BeanMapper.of(type, prefix));
    }

    /**
     * Returns a mapper for the given bean class
     *
     * @param <T> the type to find the mapper for
     * @param type the mapped class
     * @return a mapper for the given bean class
     */
    public static <T> RowMapper<T> of(Class<T> type) {
        return BeanMapper.of(type, DEFAULT_PREFIX);
    }

    /**
     * Returns a mapper for the given bean class
     *
     * @param <T> the type to find the mapper for
     * @param type the mapped class
     * @param prefix the column name prefix for each mapped bean property
     * @return a mapper for the given bean class
     */
    public static <T> RowMapper<T> of(Class<T> type, String prefix) {
        return new BeanMapper<>(type, prefix);
    }

    private BeanMapper(Class<T> type, PojoProperties<T> properties, String prefix) {
        super(type, properties, prefix);
        strictColumnTypeMapping = false;
    }

    @SuppressWarnings("unchecked")
    BeanMapper(Class<T> type, String prefix) {
        this(type, (PojoProperties<T>) BeanPropertiesFactory.propertiesFor(type), prefix);
    }

    @Override
    protected BeanMapper<?> createNestedMapper(StatementContext ctx, PojoProperty<T> property, String nestedPrefix) {
        return new BeanMapper<>(GenericTypes.getErasedType(property.getQualifiedType().getType()), nestedPrefix);
    }
}

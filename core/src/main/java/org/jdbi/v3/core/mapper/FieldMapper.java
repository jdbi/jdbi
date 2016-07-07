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

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.util.bean.ColumnNameMappingStrategy;

/**
 * A row mapper which maps the fields in a statement into an object. This uses
 * the reflection to set the fields on the object including its super class fields,
 * it does not support nested properties.
 *
 * The class must have a default constructor.
 */
public class FieldMapper<T> implements RowMapper<T>
{
    private final Class<T> type;
    private final ConcurrentMap<String, Optional<Field>> fieldByNameCache = new ConcurrentHashMap<>();
    private final Collection<ColumnNameMappingStrategy> nameMappingStrategies;

    public FieldMapper(Class<T> type)
    {
        this(type, BeanMapper.DEFAULT_STRATEGIES);
    }

    public FieldMapper(Class<T> type, Collection<ColumnNameMappingStrategy> nameMappingStrategies)
    {
        this.type = type;
        this.nameMappingStrategies = Collections.unmodifiableList(new ArrayList<>(nameMappingStrategies));
    }

    @Override
    public T map(ResultSet rs, StatementContext ctx)
            throws SQLException
    {
        T bean;
        try {
            bean = type.newInstance();
        }
        catch (Exception e) {
            throw new IllegalArgumentException(String.format("A bean, %s, was mapped " +
                    "which was not instantiable", type.getName()), e);
        }

        ResultSetMetaData metadata = rs.getMetaData();

        for (int i = 1; i <= metadata.getColumnCount(); ++i) {
            String name = metadata.getColumnLabel(i).toLowerCase();

            Optional<Field> maybeField = fieldByNameCache.computeIfAbsent(name, this::fieldByColumn);

            if (!maybeField.isPresent()) {
                continue;
            }

            final Field field = maybeField.get();
            final Type type = field.getGenericType();
            final Object value;
            final Optional<ColumnMapper<?>> mapper = ctx.findColumnMapperFor(type);

            if (mapper.isPresent()) {
                value = mapper.get().map(rs, i, ctx);
            }
            else {
                value = rs.getObject(i);
            }

            try
            {
                field.setAccessible(true);
                field.set(bean, value);
            }
            catch (IllegalAccessException e) {
                throw new IllegalArgumentException(String.format("Unable to access " +
                        "property, %s", name), e);
            }
        }

        return bean;
    }

    private Optional<Field> fieldByColumn(String columnName)
    {
        Class<?> aClass = type;
        while(aClass != null) {
            for (Field field : aClass.getDeclaredFields()) {
                for (ColumnNameMappingStrategy strategy : nameMappingStrategies) {
                    if (strategy.nameMatches(field.getName(), columnName)) {
                        return Optional.of(field);
                    }
                }
            }
            aClass = aClass.getSuperclass();
        }
        return Optional.empty();
    }
}


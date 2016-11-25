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

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;

/**
 * A row mapper which maps the columns in a statement into an object, using reflection
 * to set fields on the object. All declared fields of the class and its superclasses
 * may be set. Nested properties are not supported.
 *
 * The mapped class must have a default constructor.
 */
public class FieldMapper<T> implements RowMapper<T>
{
    /**
     * Returns a mapper factory that maps to the given bean class
     *
     * @param type the mapped class
     * @return a mapper factory that maps to the given bean class
     */
    public static RowMapperFactory of(Class<?> type) {
        return of(type, new FieldMapper<>(type));
    }

    /**
     * Returns a mapper factory that maps to the given bean class
     *
     * @param type the mapped class
     * @param prefix the column name prefix for each mapped field
     * @return a mapper factory that maps to the given bean class
     */
    public static RowMapperFactory of(Class<?> type, String prefix) {
        return of(type, new FieldMapper<>(type, prefix));
    }

    private static RowMapperFactory of(Class<?> type, RowMapper<?> mapper) {
        return (t, ctx) -> t == type
                ? Optional.of(mapper)
                : Optional.empty();
    }

    static final String DEFAULT_PREFIX = "";

    private final Class<T> type;
    private final String prefix;
    private final ConcurrentMap<String, Optional<Field>> fieldByNameCache = new ConcurrentHashMap<>();

    public FieldMapper(Class<T> type)
    {
        this(type, DEFAULT_PREFIX);
    }

    public FieldMapper(Class<T> type, String prefix)
    {
        this.type = type;
        this.prefix = prefix;
    }

    @Override
    public T map(ResultSet rs, StatementContext ctx)
            throws SQLException
    {
        T obj;
        try {
            obj = type.newInstance();
        }
        catch (Exception e) {
            throw new IllegalArgumentException(String.format("A bean, %s, was mapped " +
                    "which was not instantiable", type.getName()), e);
        }

        ResultSetMetaData metadata = rs.getMetaData();
        List<ColumnNameMatcher> columnNameMatchers = ctx.getConfig(ReflectionMappers.class).getColumnNameMatchers();

        for (int i = 1; i <= metadata.getColumnCount(); ++i) {
            String name = metadata.getColumnLabel(i).toLowerCase();

            if (prefix.length() > 0) {
                if (name.length() > prefix.length() &&
                        name.regionMatches(true, 0, prefix, 0, prefix.length())) {
                    name = name.substring(prefix.length());
                }
                else {
                    continue;
                }
            }

            Optional<Field> maybeField = fieldByNameCache.computeIfAbsent(name, n -> fieldByColumn(n, columnNameMatchers));

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
                field.set(obj, value);
            }
            catch (IllegalAccessException e) {
                throw new IllegalArgumentException(String.format("Unable to access " +
                        "property, %s", name), e);
            }
        }

        return obj;
    }

    private Optional<Field> fieldByColumn(String columnName, List<ColumnNameMatcher> columnNameMatchers)
    {
        Class<?> aClass = type;
        while(aClass != null) {
            for (Field field : aClass.getDeclaredFields()) {
                String paramName = paramName(field);
                for (ColumnNameMatcher strategy : columnNameMatchers) {
                    if (strategy.columnNameMatches(columnName, paramName)) {
                        return Optional.of(field);
                    }
                }
            }
            aClass = aClass.getSuperclass();
        }
        return Optional.empty();
    }

    private String paramName(Field field)
    {
        return Optional.ofNullable(field.getAnnotation(ColumnName.class))
                .map(ColumnName::value)
                .orElseGet(field::getName);
    }
}


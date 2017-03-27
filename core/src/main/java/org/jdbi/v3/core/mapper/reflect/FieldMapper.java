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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jdbi.v3.core.statement.StatementContext;
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
    public static RowMapperFactory factory(Class<?> type) {
        return RowMapperFactory.of(type, new FieldMapper<>(type));
    }

    /**
     * Returns a mapper factory that maps to the given bean class
     *
     * @param type the mapped class
     * @param prefix the column name prefix for each mapped field
     * @return a mapper factory that maps to the given bean class
     */
    public static RowMapperFactory factory(Class<?> type, String prefix) {
        return RowMapperFactory.of(type, new FieldMapper<>(type, prefix));
    }

    /**
     * Returns a mapper for the given bean class
     *
     * @param type the mapped class
     * @return a mapper for the given bean class
     */
    public static <T> RowMapper<T> of(Class<T> type) {
        return new FieldMapper<>(type);
    }

    /**
     * Returns a mapper for the given bean class
     *
     * @param type the mapped class
     * @param prefix the column name prefix for each mapped field
     * @return a mapper for the given bean class
     */
    public static <T> RowMapper<T> of(Class<T> type, String prefix) {
        return new FieldMapper<>(type, prefix);
    }

    static final String DEFAULT_PREFIX = "";

    private final Class<T> type;
    private final String prefix;
    private final ConcurrentMap<String, Optional<Field>> fieldByNameCache = new ConcurrentHashMap<>();

    private FieldMapper(Class<T> type)
    {
        this(type, DEFAULT_PREFIX);
    }

    private FieldMapper(Class<T> type, String prefix)
    {
        this.type = type;
        this.prefix = prefix;
    }

    @Override
    public T map(ResultSet rs, StatementContext ctx) throws SQLException {
        return specialize(rs, ctx).map(rs, ctx);
    }

    @Override
    public RowMapper<T> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        List<Integer> columnNumbers = new ArrayList<>();
        List<ColumnMapper<?>> mappers = new ArrayList<>();
        List<Field> fields = new ArrayList<>();

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
            final ColumnMapper<?> mapper = ctx.findColumnMapperFor(type)
                    .orElse((r, n, c) -> r.getObject(n));

            columnNumbers.add(i);
            mappers.add(mapper);
            fields.add(field);
        }

        if (columnNumbers.isEmpty() && metadata.getColumnCount() > 0) {
            throw new IllegalArgumentException(String.format("Mapping fields for type %s " +
                    "didn't find any matching columns in result set", type));
        }

        if (    ctx.getConfig(ReflectionMappers.class).isStrictMatching() &&
                columnNumbers.size() != metadata.getColumnCount()) {
            throw new IllegalArgumentException(String.format("Mapping fields for type %s " +
                    "only matched properties for %s of %s columns", type,
                    columnNumbers.size(), metadata.getColumnCount()));
        }


        return (r, c) -> {
            T obj;
            try {
                obj = type.newInstance();
            }
            catch (Exception e) {
                throw new IllegalArgumentException(String.format("A type, %s, was mapped " +
                        "which was not instantiable", type.getName()), e);
            }

            for (int i = 0; i < columnNumbers.size(); i++) {
                int columnNumber = columnNumbers.get(i);
                ColumnMapper<?> mapper = mappers.get(i);
                Field field = fields.get(i);

                Object value = mapper.map(rs, columnNumber, ctx);
                try {
                    field.setAccessible(true);
                    field.set(obj, value);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(String.format("Unable to access " +
                            "property, %s", field.getName()), e);
                }
            }
            return obj;
        };
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


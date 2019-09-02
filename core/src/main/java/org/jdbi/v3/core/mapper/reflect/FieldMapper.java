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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.PropagateNull;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.SingleColumnMapper;
import org.jdbi.v3.core.mapper.reflect.internal.PojoMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.statement.StatementContext;

import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.anyColumnsStartWithPrefix;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.findColumnIndex;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.getColumnNames;

/**
 * A row mapper which maps the columns in a statement into an object, using reflection
 * to set fields on the object. All declared fields of the class and its superclasses
 * may be set. Nested properties are not supported.
 *
 * The mapped class must have a default constructor.
 */
public class FieldMapper<T> implements RowMapper.Specialized<T> {
    private static final String DEFAULT_PREFIX = "";

    private static final String NO_MATCHING_COLUMNS =
        "Mapping fields for type %s didn't find any matching columns in result set";

    private static final String UNMATCHED_COLUMNS_STRICT =
        "Mapping type %s could not match fields for columns: %s";

    private static final String TYPE_NOT_INSTANTIABLE =
        "A type, %s, was mapped which was not instantiable";
    private static final String CANNOT_ACCESS_PROPERTY = "Unable to access property, %s";

    /**
     * Returns a mapper factory that maps to the given bean class
     *
     * @param type the mapped class
     * @return a mapper factory that maps to the given bean class
     */
    public static RowMapperFactory factory(Class<?> type) {
        return RowMapperFactory.of(type, FieldMapper.of(type));
    }

    /**
     * Returns a mapper factory that maps to the given bean class
     *
     * @param type the mapped class
     * @param prefix the column name prefix for each mapped field
     * @return a mapper factory that maps to the given bean class
     */
    public static RowMapperFactory factory(Class<?> type, String prefix) {
        return RowMapperFactory.of(type, FieldMapper.of(type, prefix));
    }

    /**
     * Returns a mapper for the given bean class
     *
     * @param <T> the type to map
     * @param type the mapped class
     * @return a mapper for the given bean class
     */
    public static <T> RowMapper<T> of(Class<T> type) {
        return FieldMapper.of(type, DEFAULT_PREFIX);
    }

    /**
     * Returns a mapper for the given bean class
     *
     * @param <T> the type to map
     * @param type the mapped class
     * @param prefix the column name prefix for each mapped field
     * @return a mapper for the given bean class
     */
    public static <T> RowMapper<T> of(Class<T> type, String prefix) {
        return new FieldMapper<>(type, prefix);
    }

    private final Class<T> type;
    private final String prefix;
    private final Map<Field, FieldMapper<?>> nestedMappers = new ConcurrentHashMap<>();

    private FieldMapper(Class<T> type, String prefix) {
        this.type = type;
        this.prefix = prefix.toLowerCase();
    }

    @Override
    public RowMapper<T> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        final List<String> columnNames = getColumnNames(rs);
        final List<ColumnNameMatcher> columnNameMatchers =
                ctx.getConfig(ReflectionMappers.class).getColumnNameMatchers();
        final List<String> unmatchedColumns = new ArrayList<>(columnNames);

        RowMapper<T> mapper = specialize0(ctx, columnNames, columnNameMatchers, unmatchedColumns)
            .orElseThrow(() -> new IllegalArgumentException(String.format(NO_MATCHING_COLUMNS, type)));

        if (ctx.getConfig(ReflectionMappers.class).isStrictMatching()
            && anyColumnsStartWithPrefix(unmatchedColumns, prefix, columnNameMatchers)) {
            throw new IllegalArgumentException(
                String.format(UNMATCHED_COLUMNS_STRICT, type.getSimpleName(), unmatchedColumns));
        }

        return mapper;
    }

    private Optional<RowMapper<T>> specialize0(StatementContext ctx,
                                               List<String> columnNames,
                                               List<ColumnNameMatcher> columnNameMatchers,
                                               List<String> unmatchedColumns) {
        final List<FieldData> fields = new ArrayList<>();

        for (Class<?> aType = type; aType != null; aType = aType.getSuperclass()) {
            for (Field field : aType.getDeclaredFields()) {
                Nested anno = field.getAnnotation(Nested.class);
                if (anno == null) {
                    String paramName = prefix + paramName(field);

                    findColumnIndex(paramName, columnNames, columnNameMatchers, () -> debugName(field))
                        .ifPresent(index -> {
                            QualifiedType<?> fieldType = QualifiedType.of(field.getGenericType())
                                .withAnnotations(ctx.getConfig(Qualifiers.class).findFor(field));
                            @SuppressWarnings("unchecked")
                            ColumnMapper<?> mapper = ctx.findColumnMapperFor(fieldType)
                                .orElse((ColumnMapper) (r, n, c) -> r.getObject(n));
                            fields.add(new FieldData(field, new SingleColumnMapper<>(mapper, index + 1)));
                            unmatchedColumns.remove(columnNames.get(index));
                        });
                } else {
                    String nestedPrefix = prefix + anno.value().toLowerCase();

                    if (anyColumnsStartWithPrefix(columnNames, nestedPrefix, columnNameMatchers)) {
                        nestedMappers
                            .computeIfAbsent(field, f -> new FieldMapper<>(field.getType(), nestedPrefix))
                            .specialize0(ctx, columnNames, columnNameMatchers, unmatchedColumns)
                            .ifPresent(mapper ->
                                fields.add(new FieldData(field, mapper)));
                    }
                }
            }
        }

        if (fields.isEmpty() && !columnNames.isEmpty()) {
            return Optional.empty();
        }

        Collections.sort(fields, Comparator.comparing(f -> f.propagateNull ? 1 : 0));

        final Optional<String> nullMarkerColumn =
                Optional.ofNullable(type.getAnnotation(PropagateNull.class))
                    .map(PropagateNull::value);
        return Optional.of((r, c) -> {
            if (PojoMapper.propagateNull(r, nullMarkerColumn)) {
                return null;
            }
            T obj = construct();

            for (FieldData f : fields) {
                Object value = f.mapper.map(r, ctx);
                if (f.propagateNull && (value == null || f.isPrimitive && r.wasNull())) {
                    return null;
                }
                writeField(obj, f.field, value);
            }

            return obj;
        });
    }

    private static String paramName(Field field) {
        return Optional.ofNullable(field.getAnnotation(ColumnName.class))
                .map(ColumnName::value)
                .orElseGet(field::getName);
    }

    private String debugName(Field field) {
        return String.format("%s.%s", type.getSimpleName(), field.getName());
    }

    private T construct() {
        try {
            return type.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format(TYPE_NOT_INSTANTIABLE, type.getName()), e);
        }
    }

    private void writeField(T obj, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format(CANNOT_ACCESS_PROPERTY, field.getName()), e);
        }
    }

    private static class FieldData {
        FieldData(Field field, RowMapper<?> mapper) {
            this.field = field;
            this.mapper = mapper;
            propagateNull = field.getAnnotation(PropagateNull.class) != null;
            isPrimitive = field.getType().isPrimitive();
        }
        final Field field;
        final RowMapper<?> mapper;
        final boolean propagateNull;
        final boolean isPrimitive;
    }
}


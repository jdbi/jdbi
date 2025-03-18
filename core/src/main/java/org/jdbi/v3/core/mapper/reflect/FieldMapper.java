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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jdbi.v3.core.annotation.internal.JdbiAnnotations;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.PropagateNull;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.SingleColumnMapper;
import org.jdbi.v3.core.mapper.reflect.internal.NullDelegatingMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.statement.StatementContext;

import static java.lang.String.format;

import static org.jdbi.v3.core.mapper.ColumnMapper.getDefaultColumnMapper;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.addPropertyNamePrefix;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.anyColumnsStartWithPrefix;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.findColumnIndex;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.getColumnNames;

/**
 * A row mapper which maps the columns in a statement into an object, using reflection
 * to set fields on the object. All declared fields of the class and its superclasses
 * may be set. Nested properties are supported via the {@link Nested} annotation.
 *
 * The mapped class must have a default constructor.
 */
public final class FieldMapper<T> implements RowMapper<T> {
    private static final String DEFAULT_PREFIX = "";

    /**
     * Returns a mapper factory that maps to the given bean class
     *
     * @param type the mapped class
     * @return a mapper factory that maps to the given bean class
     */
    public static RowMapperFactory factory(Class<?> type) {
        return RowMapperFactory.of(type, of(type));
    }

    /**
     * Returns a mapper factory that maps to the given bean class
     *
     * @param type the mapped class
     * @param prefix the column name prefix for each mapped field
     * @return a mapper factory that maps to the given bean class
     */
    public static RowMapperFactory factory(Class<?> type, String prefix) {
        return RowMapperFactory.of(type, of(type, prefix));
    }

    /**
     * Returns a mapper for the given bean class
     *
     * @param <T> the type to map
     * @param type the mapped class
     * @return a mapper for the given bean class
     */
    public static <T> RowMapper<T> of(Class<T> type) {
        return of(type, DEFAULT_PREFIX);
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
        this.prefix = prefix;
    }

    @Override
    public T map(ResultSet rs, StatementContext ctx) throws SQLException {
        return specialize(rs, ctx).map(rs, ctx);
    }

    @Override
    public RowMapper<T> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        final UnaryOperator<String> caseChange = ctx.getConfig(ReflectionMappers.class).getCaseChange();
        final List<String> columnNames = getColumnNames(rs, caseChange);
        final List<ColumnNameMatcher> columnNameMatchers = ctx.getConfig(ReflectionMappers.class).getColumnNameMatchers();
        final List<String> unmatchedColumns = new ArrayList<>(columnNames);

        RowMapper<T> mapper = createSpecializedRowMapper(ctx, columnNames, columnNameMatchers, unmatchedColumns, Function.identity())
            .orElseThrow(() -> new IllegalArgumentException(format("Mapping fields for type %s didn't find any matching columns in result set", type)));

        if (ctx.getConfig(ReflectionMappers.class).isStrictMatching()
            && anyColumnsStartWithPrefix(unmatchedColumns, prefix, columnNameMatchers)) {
            throw new IllegalArgumentException(
                format("Mapping type %s could not match fields for columns: %s", type.getSimpleName(), unmatchedColumns));
        }

        return mapper;
    }

    private <R> Optional<RowMapper<R>> createSpecializedRowMapper(StatementContext ctx,
                                               List<String> columnNames,
                                               List<ColumnNameMatcher> columnNameMatchers,
                                               List<String> unmatchedColumns, Function<T, R> postProcessor) {
        final List<FieldData> fields = new ArrayList<>();

        for (Class<?> aType = type; aType != null; aType = aType.getSuperclass()) {
            for (Field field : aType.getDeclaredFields()) {
                Nested nested = field.getAnnotation(Nested.class);
                if (Modifier.isStatic(field.getModifiers()) || !JdbiAnnotations.isMapped(field)) {
                    continue;
                }

                if (nested == null) {
                    String paramName = addPropertyNamePrefix(prefix, paramName(field));

                    findColumnIndex(paramName, columnNames, columnNameMatchers, () -> debugName(field))
                        .ifPresent(index -> {
                            QualifiedType<?> fieldType = QualifiedType.of(field.getGenericType())
                                .withAnnotations(ctx.getConfig(Qualifiers.class).findFor(field));
                            ColumnMapper<?> mapper = ctx.findColumnMapperFor(fieldType)
                                .orElse(getDefaultColumnMapper());
                            fields.add(new FieldData(field, new SingleColumnMapper<>(mapper, index + 1)));
                            unmatchedColumns.remove(columnNames.get(index));
                        });
                } else {
                    String nestedPrefix = addPropertyNamePrefix(prefix, nested.value());

                    if (anyColumnsStartWithPrefix(columnNames, nestedPrefix, columnNameMatchers)) {
                        Optional<? extends RowMapper<?>> nestedMapper;
                        if (field.getType().equals(Optional.class)) {
                            Class<?> rawType = GenericTypes.findGenericParameter(field.getGenericType(), Optional.class)
                                .map(GenericTypes::getErasedType)
                                .orElseThrow(() -> new IllegalArgumentException(
                                    format("Could not determine the type of the Optional field %s", field.getName())));
                            nestedMapper = nestedMappers
                                .computeIfAbsent(field, f -> new FieldMapper<>(rawType, nestedPrefix))
                                .createSpecializedRowMapper(ctx, columnNames, columnNameMatchers, unmatchedColumns, Optional::ofNullable);
                        } else {
                            nestedMapper = nestedMappers
                                .computeIfAbsent(field, f -> new FieldMapper<>(field.getType(), nestedPrefix))
                                .createSpecializedRowMapper(ctx, columnNames, columnNameMatchers, unmatchedColumns, Function.identity());
                        }

                        nestedMapper.ifPresent(mapper ->
                                fields.add(new FieldData(field, mapper)));
                    }
                }
            }
        }

        if (fields.isEmpty() && !columnNames.isEmpty()) {
            return Optional.empty();
        }

        fields.sort(Comparator.comparing(f -> f.propagateNull ? 1 : 0));

        final ReflectionMappers reflectionConfig = ctx.getConfig(ReflectionMappers.class);
        fields.forEach(fieldData ->
                reflectionConfig.makeAccessible(fieldData.field));

        final Constructor<T> constructor;
        try {
            constructor = reflectionConfig.makeAccessible(type.getDeclaredConstructor());
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(format("A type, %s, was mapped which was not instantiable", type.getName()), e);
        }
        RowMapper<R> boundMapper = new BoundFieldMapper<>(constructor, fields, postProcessor);
        OptionalInt propagateNullColumnIndex = locatePropagateNullColumnIndex(columnNames, columnNameMatchers);

        if (propagateNullColumnIndex.isPresent()) {
            return Optional.of(new NullDelegatingMapper<>(propagateNullColumnIndex.getAsInt() + 1, boundMapper));
        } else {
            return Optional.of(boundMapper);
        }
    }

    private OptionalInt locatePropagateNullColumnIndex(List<String> columnNames, List<ColumnNameMatcher> columnNameMatchers) {
        Optional<String> propagateNullColumn =
            Optional.ofNullable(type.getAnnotation(PropagateNull.class))
                .map(PropagateNull::value)
                .map(name -> addPropertyNamePrefix(prefix, name));

        if (!propagateNullColumn.isPresent()) {
            return OptionalInt.empty();
        }

        return findColumnIndex(propagateNullColumn.get(), columnNames, columnNameMatchers, propagateNullColumn::get);
    }

    private static String paramName(Field field) {
        return Optional.ofNullable(field.getAnnotation(ColumnName.class))
            .map(ColumnName::value)
            .orElseGet(field::getName);
    }

    private String debugName(Field field) {
        return format("%s.%s", type.getSimpleName(), field.getName());
    }

    public static boolean checkPropagateNullAnnotation(Field field) {
        final Optional<String> propagateNullValue = Optional.ofNullable(field.getAnnotation(PropagateNull.class)).map(PropagateNull::value);
        propagateNullValue.ifPresent(v -> {
            if (!v.isEmpty()) {
                throw new IllegalArgumentException(format("@PropagateNull does not support a value (%s) on a field (%s)", v, field.getName()));
            }
        });

        return propagateNullValue.isPresent();
    }

    private static class FieldData {

        FieldData(Field field, RowMapper<?> mapper) {
            this.field = field;
            this.mapper = mapper;
            propagateNull = checkPropagateNullAnnotation(field);
            isPrimitive = field.getType().isPrimitive();
        }

        final Field field;
        final RowMapper<?> mapper;
        final boolean propagateNull;
        final boolean isPrimitive;
    }

    class BoundFieldMapper<R> implements RowMapper<R> {
        private final Constructor<T> constructor;
        private final List<FieldData> fields;
        private final Function<T, R> postProcessor;

        BoundFieldMapper(Constructor<T> constructor, List<FieldData> fields, Function<T, R> postProcessor) {
            this.constructor = constructor;
            this.fields = fields;
            this.postProcessor = postProcessor;
        }

        @Override
        public R map(ResultSet rs, StatementContext ctx) throws SQLException {
            T obj = construct();

            for (FieldData f : fields) {
                Object value = f.mapper.map(rs, ctx);
                boolean wasNull = (value == null || (f.isPrimitive && rs.wasNull()));
                if (f.propagateNull && wasNull) {
                    return postProcessor.apply(null);
                }
                writeField(obj, f.field, value);
            }

            return postProcessor.apply(obj);
        }

        private T construct() {
            try {
                return constructor.newInstance();
            } catch (ReflectiveOperationException | SecurityException e) {
                throw new IllegalArgumentException(format("A type, %s, was mapped which was not instantiable", type.getName()), e);
            }
        }

        private void writeField(T obj, Field field, Object value) {
            try {
                field.set(obj, value);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(format("Unable to access property, %s", field.getName()), e);
            }
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", BoundFieldMapper.class.getSimpleName() + "[", "]")
                .add("type=" + type.getSimpleName())
                .add("prefix=" + prefix)
                .toString();
        }
    }
}


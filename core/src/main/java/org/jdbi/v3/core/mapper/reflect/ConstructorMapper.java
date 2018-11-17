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

import java.beans.ConstructorProperties;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.SingleColumnMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.StatementContext;

import static org.jdbi.v3.core.mapper.reflect.JdbiConstructors.findConstructorFor;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.anyColumnsStartWithPrefix;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.findColumnIndex;
import static org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.getColumnNames;
import static org.jdbi.v3.core.qualifier.Qualifiers.getQualifiers;

/**
 * A row mapper which maps the fields in a result set into a constructor. The default implementation will perform a
 * case insensitive mapping between the constructor parameter names and the column labels,
 * also considering camel-case to underscores conversion.
 * <p>
 * This mapper respects {@link Nested} annotations on constructor parameters.
 * <p>
 * Constructor parameters annotated as {@code @Nullable} may be omitted from the result set without
 * error. Any annotation named "Nullable" is respected--nay, worshipped--no matter which package it is from.
 */
public class ConstructorMapper<T> implements RowMapper<T> {
    private static final String DEFAULT_PREFIX = "";

    private static final String UNMATCHED_CONSTRUCTOR_PARAMETERS =
        "Constructor '%s' could not match any parameter to any columns in the result set. "
            + "Verify that the Java compiler is configured to emit parameter names, "
            + "that your result set has the columns expected, annotate the "
            + "parameter names explicitly with @ColumnName, or annotate nullable parameters as @Nullable";

    private static final String UNMATCHED_CONSTRUCTOR_PARAMETER =
        "Constructor '%s' parameter '%s' has no matching columns in the result set. "
            + "Verify that the Java compiler is configured to emit parameter names, "
            + "that your result set has the columns expected, annotate the "
            + "parameter names explicitly with @ColumnName, or annotate nullable parameters as @Nullable";

    private static final String UNMATCHED_COLUMNS_STRICT =
        "Mapping constructor-injected type %s could not match parameters for columns: %s";

    private static final String MISSING_COLUMN_MAPPER =
        "Could not find column mapper for type '%s' of parameter '%s' for constructor '%s'";

    /**
     * Use the only declared constructor to map a class.
     *
     * @param clazz the class to find a constructor of
     * @return the factory
     */
    public static RowMapperFactory factory(Class<?> clazz) {
        return RowMapperFactory.of(clazz, ConstructorMapper.of(clazz));
    }

    /**
     * Use the only declared constructor to map a class.
     *
     * @param clazz the class to find a constructor of
     * @param prefix a prefix for the parameter names
     * @return the factory
     */
    public static RowMapperFactory factory(Class<?> clazz, String prefix) {
        return RowMapperFactory.of(clazz, ConstructorMapper.of(clazz, prefix));
    }

    /**
     * Use a {@code Constructor<T>} to map its declaring type.
     *
     * @param constructor the constructor to invoke
     * @return the factory
     */
    public static RowMapperFactory factory(Constructor<?> constructor) {
        return RowMapperFactory.of(constructor.getDeclaringClass(), ConstructorMapper.of(constructor));
    }

    /**
     * Use a {@code Constructor<T>} to map its declaring type.
     *
     * @param constructor the constructor to invoke
     * @param prefix a prefix to the constructor parameter names
     * @return the factory
     */
    public static RowMapperFactory factory(Constructor<?> constructor, String prefix) {
        return RowMapperFactory.of(constructor.getDeclaringClass(), ConstructorMapper.of(constructor, prefix));
    }

    /**
     * Return a ConstructorMapper for the given type.
     *
     * @param <T>  the type to map
     * @param type the mapped type
     * @return the mapper
     */
    public static <T> RowMapper<T> of(Class<T> type) {
        return ConstructorMapper.of(findConstructorFor(type));
    }

    /**
     * Return a ConstructorMapper for the given type and prefix.
     *
     * @param <T>    the type to map
     * @param type   the mapped type
     * @param prefix the column name prefix
     * @return the mapper
     */
    public static <T> RowMapper<T> of(Class<T> type, String prefix) {
        return ConstructorMapper.of(findConstructorFor(type), prefix);
    }

    /**
     * Return a ConstructorMapper using the given constructor
     *
     * @param <T> the type to map
     * @param constructor the constructor to be used in mapping
     * @return the mapper
     */
    public static <T> RowMapper<T> of(Constructor<T> constructor) {
        return ConstructorMapper.of(constructor, DEFAULT_PREFIX);
    }

    /**
     * Instantiate a ConstructorMapper using the given constructor and prefix
     *
     * @param <T> the type to map
     * @param constructor the constructor to be used in mapping
     * @param prefix      the column name prefix
     * @return the mapper
     */
    public static <T> RowMapper<T> of(Constructor<T> constructor, String prefix) {
        return new ConstructorMapper<>(constructor, prefix);
    }

    private final Constructor<T> constructor;
    private final String prefix;
    private final ConstructorProperties constructorProperties;
    private final Map<Parameter, ConstructorMapper<?>> nestedMappers = new ConcurrentHashMap<>();

    private ConstructorMapper(Constructor<T> constructor, String prefix) {
        this.constructor = constructor;
        this.prefix = prefix.toLowerCase();
        this.constructorProperties = constructor.getAnnotation(ConstructorProperties.class);
    }

    @Override
    public T map(ResultSet rs, StatementContext ctx) throws SQLException {
        return specialize(rs, ctx).map(rs, ctx);
    }

    @Override
    public RowMapper<T> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        final List<String> columnNames = getColumnNames(rs);
        final List<ColumnNameMatcher> columnNameMatchers =
                ctx.getConfig(ReflectionMappers.class).getColumnNameMatchers();
        final List<String> unmatchedColumns = new ArrayList<>(columnNames);

        RowMapper<T> mapper = specialize0(ctx, columnNames, columnNameMatchers, unmatchedColumns)
            .orElseThrow(() -> new IllegalArgumentException(String.format(
                UNMATCHED_CONSTRUCTOR_PARAMETERS, constructor)));

        if (ctx.getConfig(ReflectionMappers.class).isStrictMatching()
            && anyColumnsStartWithPrefix(unmatchedColumns, prefix, columnNameMatchers)) {

            throw new IllegalArgumentException(
                String.format(UNMATCHED_COLUMNS_STRICT, constructor.getDeclaringClass().getSimpleName(), unmatchedColumns));
        }

        return mapper;
    }

    private Optional<RowMapper<T>> specialize0(StatementContext ctx,
                                               List<String> columnNames,
                                               List<ColumnNameMatcher> columnNameMatchers,
                                               List<String> unmatchedColumns) {
        final int count = constructor.getParameterCount();
        final Parameter[] parameters = constructor.getParameters();

        final RowMapper<?>[] mappers = new RowMapper<?>[count];

        boolean matchedColumns = false;
        final List<String> unmatchedParameters = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final Parameter parameter = parameters[i];

            boolean nullable = isNullable(parameter);
            Nested anno = parameter.getAnnotation(Nested.class);
            if (anno == null) {
                final String paramName = prefix + paramName(parameters, i, constructorProperties);

                final OptionalInt columnIndex = findColumnIndex(paramName, columnNames, columnNameMatchers,
                    () -> debugName(parameter));

                if (columnIndex.isPresent()) {
                    int colIndex = columnIndex.getAsInt();
                    final QualifiedType type = QualifiedType.of(
                        parameter.getParameterizedType(),
                        getQualifiers(parameter));
                    mappers[i] = ctx.findColumnMapperFor(type)
                        .map(mapper -> new SingleColumnMapper<>(mapper, colIndex + 1))
                        .orElseThrow(() -> new IllegalArgumentException(
                            String.format(MISSING_COLUMN_MAPPER, type, paramName, constructor)));

                    matchedColumns = true;
                    unmatchedColumns.remove(columnNames.get(colIndex));
                } else if (nullable) {
                    mappers[i] = (r, c) -> null;
                } else {
                    unmatchedParameters.add(paramName);
                }
            } else {
                final String nestedPrefix = prefix + anno.value();

                final Optional<? extends RowMapper<?>> nestedMapper = nestedMappers
                    .computeIfAbsent(parameter, p ->
                        new ConstructorMapper<>(findConstructorFor(p.getType()), nestedPrefix))
                    .specialize0(ctx, columnNames, columnNameMatchers, unmatchedColumns);

                if (nestedMapper.isPresent()) {
                    mappers[i] = nestedMapper.get();
                    matchedColumns = true;
                } else if (nullable) {
                    mappers[i] = (r, c) -> null;
                } else {
                    unmatchedParameters.add(paramName(parameters, i, constructorProperties));
                }
            }
        }

        if (!matchedColumns) {
            return Optional.empty();
        }

        if (!unmatchedParameters.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                UNMATCHED_CONSTRUCTOR_PARAMETER, constructor, unmatchedParameters));
        }

        return Optional.of((r, c) -> {
            final Object[] params = new Object[count];

            for (int i = 0; i < count; i++) {
                params[i] = mappers[i].map(r, c);
            }

            return construct(params);
        });
    }

    private boolean isNullable(Parameter parameter) {
        // Any annotation named @Nullable is honored. We're nice that way.
        return Stream.of(parameter.getAnnotations())
            .map(Annotation::annotationType)
            .map(Class::getSimpleName)
            .anyMatch("Nullable"::equals);
    }

    private static String paramName(Parameter[] parameters,
                                    int position,
                                    ConstructorProperties parameterNames) {
        final Parameter parameter = parameters[position];
        ColumnName dbName = parameter.getAnnotation(ColumnName.class);
        if (dbName != null) {
            return dbName.value();
        }
        if (parameterNames != null) {
            return parameterNames.value()[position];
        }
        return parameter.getName();
    }

    private String debugName(Parameter parameter) {
        return String.format("%s constructor parameter %s",
            constructor.getDeclaringClass().getSimpleName(),
            parameter.getName());
    }

    private T construct(Object[] params) {
        try {
            return constructor.newInstance(params);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            }
            throw new RuntimeException(e);
        }
    }
}

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
package org.jdbi.core.mapper.reflect;

import java.beans.ConstructorProperties;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
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
import java.util.stream.Stream;

import org.jdbi.core.generic.GenericTypes;
import org.jdbi.core.mapper.Nested;
import org.jdbi.core.mapper.PropagateNull;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.mapper.RowMapperFactory;
import org.jdbi.core.mapper.SingleColumnMapper;
import org.jdbi.core.mapper.reflect.internal.NullDelegatingMapper;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.qualifier.Qualifiers;
import org.jdbi.core.statement.StatementContext;

import static java.lang.String.format;

import static org.jdbi.core.mapper.reflect.JdbiConstructors.findFactoryFor;
import static org.jdbi.core.mapper.reflect.ReflectionMapperUtil.addPropertyNamePrefix;
import static org.jdbi.core.mapper.reflect.ReflectionMapperUtil.anyColumnsStartWithPrefix;
import static org.jdbi.core.mapper.reflect.ReflectionMapperUtil.findColumnIndex;
import static org.jdbi.core.mapper.reflect.ReflectionMapperUtil.getColumnNames;

/**
 * A row mapper which maps the fields in a result set into a constructor. The default implementation will perform a case insensitive mapping between the
 * constructor parameter names and the column labels, also considering camel-case to underscores conversion.
 * <p>
 * This mapper respects {@link Nested} annotations on constructor parameters.
 * <p>
 * Constructor parameters annotated as {@code @Nullable} may be omitted from the result set without error. Any annotation named "Nullable" can be used, no
 * matter which package it is from.
 */
public final class ConstructorMapper<T> implements RowMapper<T> {
    private static final String DEFAULT_PREFIX = "";

    @SuppressWarnings("InlineFormatString")
    private static final String UNMATCHED_CONSTRUCTOR_PARAMETERS =
        "Instance factory '%s' could not match any parameter to any columns in the result set. "
            + "Verify that the Java compiler is configured to emit parameter names, "
            + "that your result set has the columns expected, annotate the "
            + "parameter names explicitly with @ColumnName, or annotate nullable parameters as @Nullable";

    @SuppressWarnings("InlineFormatString")
    private static final String UNMATCHED_CONSTRUCTOR_PARAMETER =
        "Instance factory '%s' parameter '%s' has no matching columns in the result set. "
            + "Verify that the Java compiler is configured to emit parameter names, "
            + "that your result set has the columns expected, annotate the "
            + "parameter names explicitly with @ColumnName, or annotate nullable parameters as @Nullable";

    /**
     * Use the only declared constructor to map a class.
     *
     * @param clazz the class to find a constructor of
     * @return the factory
     */
    public static RowMapperFactory factory(Class<?> clazz) {
        return RowMapperFactory.of(clazz, of(clazz));
    }

    /**
     * Use the only declared constructor to map a class.
     *
     * @param clazz the class to find a constructor of
     * @param prefix a prefix for the parameter names
     * @return the factory
     */
    public static RowMapperFactory factory(Class<?> clazz, String prefix) {
        return RowMapperFactory.of(clazz, of(clazz, prefix));
    }

    /**
     * Use the only declared static factory method to map a class.
     *
     * @param clazz the class to static factory method for
     * @param factoryMethodClass the type to inspect for a static factory method
     * @return the factory
     */
    public static RowMapperFactory factory(Class<?> clazz, Class<?> factoryMethodClass) {
        return RowMapperFactory.of(clazz, of(clazz, factoryMethodClass));
    }


    /**
     * Use the only declared static factory method to map a class.
     *
     * @param clazz the class to static factory method for
     * @param factoryMethodClass the type to inspect for a static factory method
     * @param prefix a prefix for the parameter names
     * @return the factory
     */
    public static RowMapperFactory factory(Class<?> clazz, Class<?> factoryMethodClass, String prefix) {
        return RowMapperFactory.of(clazz, of(clazz, factoryMethodClass, prefix));
    }

    /**
     * Use a {@code Constructor<T>} to map its declaring type.
     *
     * @param constructor the constructor to invoke
     * @return the factory
     */
    public static RowMapperFactory factory(Constructor<?> constructor) {
        return RowMapperFactory.of(constructor.getDeclaringClass(), of(constructor));
    }

    /**
     * Use a {@code Constructor<T>} to map its declaring type.
     *
     * @param constructor the constructor to invoke
     * @param prefix a prefix to the constructor parameter names
     * @return the factory
     */
    public static RowMapperFactory factory(Constructor<?> constructor, String prefix) {
        return RowMapperFactory.of(constructor.getDeclaringClass(), of(constructor, prefix));
    }

    /**
     * Return a ConstructorMapper for the given type.
     *
     * @param <T>  the type to map
     * @param type the mapped type
     * @return the mapper
     */
    public static <T> RowMapper<T> of(Class<T> type) {
        return of(type, DEFAULT_PREFIX);
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
        return new ConstructorMapper<>(findFactoryFor(type), prefix);
    }

    /**
     * Return a ConstructorMapper for the given type, based on a static factory method found in the given class.
     *
     * @param <T>  the type to map
     * @param type the mapped type
     * @param factoryMethodClass the type to inspect for a static factory method
     * @return the mapper
     */
    public static <T> RowMapper<T> of(Class<T> type, Class<?> factoryMethodClass) {
        return of(type, factoryMethodClass, DEFAULT_PREFIX);
    }

    /**
     * Return a ConstructorMapper for the given type and prefix, based on a static factory method found in the given class.
     *
     * @param <T>    the type to map
     * @param type   the mapped type
     * @param factoryMethodClass the type to inspect for a static factory method
     * @param prefix the column name prefix
     * @return the mapper
     */
    public static <T> RowMapper<T> of(Class<T> type, Class<?> factoryMethodClass, String prefix) {
        return new ConstructorMapper<>(findFactoryFor(type, factoryMethodClass), prefix);
    }

    /**
     * Return a ConstructorMapper using the given constructor
     *
     * @param <T> the type to map
     * @param constructor the constructor to be used in mapping
     * @return the mapper
     */
    public static <T> RowMapper<T> of(Constructor<T> constructor) {
        return of(constructor, DEFAULT_PREFIX);
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
        return new ConstructorMapper<>(new ConstructorInstanceFactory<>(constructor), prefix);
    }

    private final InstanceFactory<T> factory;
    private final String prefix;
    private final ConstructorProperties constructorProperties;
    private final Map<Parameter, ConstructorMapper<?>> nestedMappers = new ConcurrentHashMap<>();

    private ConstructorMapper(InstanceFactory<T> factory, String prefix) {
        this.factory = factory;
        this.prefix = prefix;
        this.constructorProperties = factory.getAnnotation(ConstructorProperties.class);
    }

    @Override
    public T map(ResultSet rs, StatementContext ctx) throws SQLException {
        return specialize(rs, ctx).map(rs, ctx);
    }

    @Override
    public RowMapper<T> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        final UnaryOperator<String> caseChange = ctx.getConfig(ReflectionMappers.class).getCaseChange();
        final List<String> columnNames = getColumnNames(rs, caseChange);
        final List<ColumnNameMatcher> columnNameMatchers =
                ctx.getConfig(ReflectionMappers.class).getColumnNameMatchers();
        final List<String> unmatchedColumns = new ArrayList<>(columnNames);

        RowMapper<T> mapper = createSpecializedRowMapper(ctx, columnNames, columnNameMatchers, unmatchedColumns, Function.identity())
            .orElseGet(() -> new UnmatchedConstructorMapper<>(format(
                UNMATCHED_CONSTRUCTOR_PARAMETERS, factory)));

        if (ctx.getConfig(ReflectionMappers.class).isStrictMatching()
            && anyColumnsStartWithPrefix(unmatchedColumns, prefix, columnNameMatchers)) {

            return new UnmatchedConstructorMapper<>(
                format("Mapping instance factory %s could not match parameters for columns: %s", factory, unmatchedColumns));
        }

        return mapper;
    }

    private <R> Optional<RowMapper<R>> createSpecializedRowMapper(StatementContext ctx,
                                                                  List<String> columnNames,
                                                                  List<ColumnNameMatcher> columnNameMatchers,
                                                                  List<String> unmatchedColumns,
                                                                  Function<T, R> postProcessor) {
        final int count = factory.getParameterCount();
        final Parameter[] parameters = factory.getParameters();
        final List<Type> types = factory.getTypes();
        boolean matchedColumns = false;
        final List<String> unmatchedParameters = new ArrayList<>();
        final List<ParameterData> paramData = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final Parameter parameter = parameters[i];
            final Type parameterType = types.get(i);
            boolean nullable = isNullable(parameter);
            Nested nested = parameter.getAnnotation(Nested.class);
            if (nested == null) {
                final String paramName = addPropertyNamePrefix(prefix, paramName(parameters, i, constructorProperties));

                final OptionalInt columnIndex = findColumnIndex(paramName, columnNames, columnNameMatchers,
                    () -> debugName(parameter));

                if (columnIndex.isPresent()) {
                    int colIndex = columnIndex.getAsInt();
                    final QualifiedType<?> type = QualifiedType.of(parameterType)
                        .withAnnotations(ctx.getConfig(Qualifiers.class).findFor(parameter));
                    paramData.add(new ParameterData(i, parameter, ctx.findColumnMapperFor(type)
                        .map(mapper -> new SingleColumnMapper<>(mapper, colIndex + 1))
                        .orElseThrow(() -> new IllegalArgumentException(
                            format("Could not find column mapper for type '%s' of parameter '%s' for instance factory '%s'", type, paramName, factory)))));

                    matchedColumns = true;
                    unmatchedColumns.remove(columnNames.get(colIndex));
                } else if (nullable) {
                    paramData.add(new ParameterData(i, parameter, null));
                } else {
                    unmatchedParameters.add(paramName);
                }
            } else {
                final String nestedPrefix = addPropertyNamePrefix(prefix, nested.value());
                Optional<? extends RowMapper<?>> nestedMapper;
                if (parameter.getType().equals(Optional.class)) {
                    Class<?> rawType = GenericTypes.findGenericParameter(parameter.getParameterizedType(), Optional.class)
                        .map(GenericTypes::getErasedType)
                        .orElseThrow(() -> new IllegalArgumentException(
                            format("Could not determine the type of the Optional parameter '%s' for instance factory '%s'", parameter.getName(), factory)));
                    ConstructorMapper<?> mapper = nestedMappers.computeIfAbsent(parameter, p ->
                            new ConstructorMapper<>(findFactoryFor(rawType), nestedPrefix));

                    nestedMapper = mapper.createSpecializedRowMapper(ctx, columnNames, columnNameMatchers, unmatchedColumns, Optional::ofNullable);
                } else {
                    nestedMapper = nestedMappers
                        .computeIfAbsent(parameter, p ->
                            new ConstructorMapper<>(findFactoryFor(p.getType()), nestedPrefix))
                        .createSpecializedRowMapper(ctx, columnNames, columnNameMatchers, unmatchedColumns, Function.identity());

                }
                if (nestedMapper.isPresent()) {
                    paramData.add(new ParameterData(i, parameter, nestedMapper.get()));
                    matchedColumns = true;
                } else if (nullable) {
                    paramData.add(new ParameterData(i, parameter, null));
                } else {
                    unmatchedParameters.add(paramName(parameters, i, constructorProperties));
                }
            }
        }

        if (!matchedColumns) {
            return Optional.empty();
        }

        paramData.sort(Comparator.comparing(
            p -> p.propagateNull ? 1 : 0));

        if (!unmatchedParameters.isEmpty()) {
            throw new IllegalArgumentException(format(
                UNMATCHED_CONSTRUCTOR_PARAMETER, factory, unmatchedParameters));
        }

        RowMapper<R> boundMapper = new BoundConstructorMapper<>(paramData, postProcessor);
        OptionalInt propagateNullColumnIndex = locatePropagateNullColumnIndex(columnNames, columnNameMatchers);

        if (propagateNullColumnIndex.isPresent()) {
            return Optional.of(new NullDelegatingMapper<>(propagateNullColumnIndex.getAsInt() + 1, boundMapper));
        } else {
            return Optional.of(boundMapper);
        }
    }

    private OptionalInt locatePropagateNullColumnIndex(List<String> columnNames, List<ColumnNameMatcher> columnNameMatchers) {
        Optional<String> propagateNullColumn =
            Optional.ofNullable(factory.getAnnotationIncludingType(PropagateNull.class))
                .map(PropagateNull::value)
                .map(name -> addPropertyNamePrefix(prefix, name));

        if (!propagateNullColumn.isPresent()) {
            return OptionalInt.empty();
        }

        return findColumnIndex(propagateNullColumn.get(), columnNames, columnNameMatchers, propagateNullColumn::get);
    }

    private boolean isNullable(Parameter parameter) {
        // Any annotation named @Nullable is honored. We're nice that way.
        // Check both parameter annotations and type-use annotations (e.g., jspecify @Nullable)
        return Stream.of(parameter.getAnnotations(), parameter.getAnnotatedType().getAnnotations())
            .flatMap(Stream::of)
            .map(Annotation::annotationType)
            .map(Class::getSimpleName)
            .anyMatch("Nullable"::equals);
    }

    private static String paramName(Parameter[] parameters,
                                    int position,
                                    ConstructorProperties parameterNames) {

        final Parameter parameter = parameters[position];
        return Optional.ofNullable(parameter.getAnnotation(ColumnName.class))
            .map(ColumnName::value)
            .orElseGet(() -> {
                if (parameterNames != null) {
                    return parameterNames.value()[position];
                } else {
                    return parameter.getName();
                }
            });
    }

    private String debugName(Parameter parameter) {
        return format("%s constructor parameter %s",
            factory.getDeclaringClass().getSimpleName(),
            parameter.getName());
    }

    private static class ParameterData {

        ParameterData(int index, Parameter parameter, RowMapper<?> mapper) {
            this.index = index;
            this.parameter = parameter;
            this.mapper = mapper;
            propagateNull = checkPropagateNullAnnotation(parameter);
            isPrimitive = parameter.getType().isPrimitive();
        }

        private static boolean checkPropagateNullAnnotation(Parameter parameter) {
            final Optional<String> propagateNullValue = Optional.ofNullable(parameter.getAnnotation(PropagateNull.class)).map(PropagateNull::value);
            propagateNullValue.ifPresent(v -> {
                if (!v.isEmpty()) {
                    throw new IllegalArgumentException(format("@PropagateNull does not support a value (%s) on a parameter (%s)", v, parameter.getName()));
                }
            });

            return propagateNullValue.isPresent();
        }

        final int index;
        final Parameter parameter;
        final RowMapper<?> mapper;
        final boolean propagateNull;
        final boolean isPrimitive;
    }

    static class UnmatchedConstructorMapper<T> implements RowMapper<T> {
        private final String message;

        UnmatchedConstructorMapper(String message) {
            this.message = message;
        }

        @Override
        public T map(ResultSet rs, StatementContext ctx) throws SQLException {
            throw new IllegalArgumentException(message);
        }
    }

    class BoundConstructorMapper<R> implements RowMapper<R> {

        private final List<ParameterData> paramData;
        private final int count;
        private final Function<T, R> postProcessor;

        BoundConstructorMapper(List<ParameterData> paramData, Function<T, R> postProcessor) {
            this.paramData = paramData;
            this.count = factory.getParameterCount();
            this.postProcessor = postProcessor;
        }

        @Override
        public R map(ResultSet rs, StatementContext ctx) throws SQLException {
            final Object[] params = new Object[count];
            for (ParameterData p : paramData) {
                params[p.index] = p.mapper == null ? null : p.mapper.map(rs, ctx);
                boolean wasNull = (params[p.index] == null || (p.isPrimitive && rs.wasNull()));
                if (p.propagateNull && wasNull) {
                    return postProcessor.apply(null);
                }
            }

            return postProcessor.apply(factory.newInstance(params));
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", BoundConstructorMapper.class.getSimpleName() + "[", "]")
                .add("type=" + factory.getDeclaringClass().getSimpleName())
                .add("prefix=" + prefix)
                .toString();
        }
    }
}

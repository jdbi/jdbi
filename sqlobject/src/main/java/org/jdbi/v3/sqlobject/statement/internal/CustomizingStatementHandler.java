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
package org.jdbi.v3.sqlobject.statement.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.result.RowReducer;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.sqlobject.Handler;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;
import org.jdbi.v3.sqlobject.statement.ParameterCustomizerFactory;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

import static java.util.stream.Stream.concat;

/**
 * Base handler for annotations' implementation classes.
 */
abstract class CustomizingStatementHandler<StatementType extends SqlStatement<StatementType>> implements Handler {
    private final List<BoundCustomizer> statementCustomizers;
    private final Class<?> sqlObjectType;
    private final Method method;

    CustomizingStatementHandler(Class<?> type, Method method) {
        this.sqlObjectType = type;
        this.method = method;

        // Include annotations on the interface's supertypes
        final Stream<BoundCustomizer> typeCustomizers = concat(superTypes(type), Stream.of(type))
            .flatMap(CustomizingStatementHandler::annotationsFor)
            .map(a -> instantiateFactory(a).createForType(a, type))
            .map(BoundCustomizer::of);

        final Stream<BoundCustomizer> methodCustomizers = annotationsFor(method)
            .map(a -> instantiateFactory(a).createForMethod(a, type, method))
            .map(BoundCustomizer::of);

        statementCustomizers = Stream.of(typeCustomizers, methodCustomizers, parameterCustomizers())
            .reduce(Stream.empty(), Stream::concat)
            .collect(Collectors.toList());
    }

    // duplicate implementation in SqlObjectFactory
    private static Stream<Class<?>> superTypes(Class<?> type) {
        Class<?>[] interfaces = type.getInterfaces();
        return concat(
            Arrays.stream(interfaces).flatMap(CustomizingStatementHandler::superTypes),
            Arrays.stream(interfaces));
    }

    private static Stream<Annotation> annotationsFor(AnnotatedElement... elements) {
        return Stream.of(elements)
                .map(AnnotatedElement::getAnnotations)
                .flatMap(Stream::of)
                .filter(a -> a.annotationType().isAnnotationPresent(SqlStatementCustomizingAnnotation.class));
    }

    private Stream<BoundCustomizer> parameterCustomizers() {
        final Parameter[] parameters = method.getParameters();

        return IntStream.range(0, parameters.length)
                .boxed()
                .flatMap(i -> eachParameterCustomizers(parameters[i], i));
    }

    private Stream<BoundCustomizer> eachParameterCustomizers(Parameter parameter,
                                                             Integer i) {

        List<BoundCustomizer> customizers = annotationsFor(parameter)
                .map(a -> instantiateFactory(a).createForParameter(a, sqlObjectType, method, parameter, i, getParameterType(parameter)))
                .<BoundCustomizer>map(c -> new BoundCustomizer() {
                    @Override
                    public void warm(ConfigRegistry config) {
                        c.warm(config);
                    }

                    @Override
                    public void apply(SqlStatement<?> stmt, Object[] args) throws SQLException {
                        c.apply(stmt, args[i]);
                    }
                })
                .collect(Collectors.toList());

        if (!customizers.isEmpty()) {
            return customizers.stream();
        }

        if (parameter.getType() == Consumer.class) {
            if (method.getReturnType() != Void.TYPE) {
                throw new IllegalStateException(
                  "SQL Object methods with a Consumer parameter must have void return type.");
            }
            return Stream.empty();
        }
        if (parameter.getType() == Function.class && this instanceof SqlCallHandler) {
            return Stream.empty();
        }

        return Stream.of(defaultParameterCustomizer(parameter, i));
    }

    /**
     * Default parameter customizer for parameters with no annotations.
     */
    private BoundCustomizer defaultParameterCustomizer(Parameter parameter,
                                                       Integer i) {
        return new BoundCustomizer() {
            @Override
            public void warm(ConfigRegistry config) {
                create(config).warm(config);
            }

            @Override
            public void apply(SqlStatement<?> stmt, Object[] args) throws SQLException {
                create(stmt.getConfig()).apply(stmt, args[i]);
            }

            private SqlStatementParameterCustomizer create(ConfigRegistry config) {
                return getDefaultParameterCustomizerFactory(config)
                        .createForParameter(sqlObjectType, method, parameter, i, getParameterType(parameter));
            }
        };
    }

    Type getParameterType(Parameter parameter) {
        return GenericTypes.resolveType(parameter.getParameterizedType(), sqlObjectType);
    }

    private static ParameterCustomizerFactory getDefaultParameterCustomizerFactory(ConfigRegistry config) {
        return config.get(SqlObjects.class).getDefaultParameterCustomizerFactory();
    }

    private static SqlStatementCustomizerFactory instantiateFactory(Annotation annotation) {
        SqlStatementCustomizingAnnotation sqlStatementCustomizingAnnotation = annotation.annotationType()
                .getAnnotation(SqlStatementCustomizingAnnotation.class);
        try {
            return sqlStatementCustomizingAnnotation.value().getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to instantiate sql statement customizer factory class " + sqlStatementCustomizingAnnotation.value(), e);
        }
    }

    @Override
    public Object invoke(Object target, Object[] args, HandleSupplier hs) {
        final Handle h = hs.getHandle();
        final String locatedSql = locateSql(h);
        final StatementType stmt = createStatement(h, locatedSql);
        final SqlObjectStatementConfiguration cfg = stmt.getConfig(SqlObjectStatementConfiguration.class);
        cfg.setArgs(args);
        configureReturner(stmt, cfg);
        applyCustomizers(stmt, args);
        return cfg.getReturner().get();
    }

    void applyCustomizers(final StatementType stmt, Object[] args) {
        statementCustomizers.forEach(b -> {
            try {
                b.apply(stmt, args);
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException(e, stmt.getContext());
            }
        });
    }

    abstract void configureReturner(StatementType stmt, SqlObjectStatementConfiguration cfg);
    abstract StatementType createStatement(Handle handle, String locatedSql);

    String locateSql(final Handle h) {
        return h.getConfig(SqlObjects.class).getSqlLocator().locate(sqlObjectType, method, h.getConfig());
    }

    Method getMethod() {
        return method;
    }

    static RowMapper<?> rowMapperFor(UseRowMapper annotation) {
        Class<? extends RowMapper<?>> mapperClass = annotation.value();
        try {
            return mapperClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new UnableToCreateStatementException("Could not create mapper " + mapperClass.getName(), e, null);
        }
    }

    static RowReducer<?, ?> rowReducerFor(UseRowReducer annotation) {
        Class<? extends RowReducer<?, ?>> reducerClass = annotation.value();
        try {
            return reducerClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new UnableToCreateStatementException("Could not create reducer " + reducerClass.getName(), e, null);
        }
    }

    /**
     * A {@link SqlStatementCustomizer} or {@link SqlStatementParameterCustomizer} that
     * is ready to apply.
     */
    private interface BoundCustomizer {
        void apply(SqlStatement<?> stmt, Object[] args) throws SQLException;
        void warm(ConfigRegistry config);

        static BoundCustomizer of(SqlStatementCustomizer inner) {
            return new BoundCustomizer() {
                @Override
                public void apply(SqlStatement<?> stmt, Object[] args) throws SQLException {
                    inner.apply(stmt);
                }

                @Override
                public void warm(ConfigRegistry config) {
                    inner.warm(config);
                }
            };
        }
    }
}

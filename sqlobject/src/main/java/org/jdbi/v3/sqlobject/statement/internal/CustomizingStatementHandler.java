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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionHandler;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.result.RowReducer;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;
import org.jdbi.v3.sqlobject.statement.ParameterCustomizerFactory;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

import static java.util.stream.Stream.concat;

import static org.jdbi.v3.core.internal.JdbiClassUtils.checkedCreateInstance;
import static org.jdbi.v3.core.internal.JdbiClassUtils.safeVarargs;
import static org.jdbi.v3.core.internal.JdbiClassUtils.superTypes;

/**
 * Base handler for annotations' implementation classes.
 */
abstract class CustomizingStatementHandler<StatementType extends SqlStatement<StatementType>> implements ExtensionHandler {

    private final List<BoundCustomizer> statementCustomizers;
    private final Class<?> sqlObjectType;
    private final Method method;

    CustomizingStatementHandler(Class<?> sqlObjectType, Method method) {
        this.sqlObjectType = sqlObjectType;
        this.method = method;
        this.statementCustomizers = new ArrayList<>();

        // type customizers, including annotations on the interface's supertypes
        concat(superTypes(sqlObjectType), Stream.of(sqlObjectType))
                .flatMap(CustomizingStatementHandler::annotationsFor)
                .map(a -> instantiateFactory(a).createForType(a, sqlObjectType))
                .map(BoundCustomizer::of)
                .forEach(statementCustomizers::add);

        // method customizers
        annotationsFor(method)
                .map(a -> instantiateFactory(a).createForMethod(a, sqlObjectType, method))
                .map(BoundCustomizer::of)
                .forEach(statementCustomizers::add);

        // parameter customizers
        parameterCustomizers().forEach(statementCustomizers::add);
    }

    @Override
    public void warm(ConfigRegistry config) {
        statementCustomizers.forEach(s -> s.warm(config));
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

    private Stream<BoundCustomizer> eachParameterCustomizers(Parameter parameter, Integer i) {
        final Type parameterType = getParameterType(parameter);
        List<BoundCustomizer> customizers = annotationsFor(parameter)
                .map(a -> instantiateFactory(a).createForParameter(a, sqlObjectType, method, parameter, i, parameterType))
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
        if (parameter.getType() == Function.class
            // SqlCallHandler supports Function argument for OutParameters
            && (this instanceof SqlCallHandler
                // SqlBatchHandler, SqlQueryHandler and SqlUpdateHandler support Function arguments
                || this instanceof SqlBatchHandler
                || this instanceof SqlQueryHandler
                || this instanceof SqlUpdateHandler)) {
            return Stream.empty();
        }

        return Stream.of(defaultParameterCustomizer(parameter, i));
    }

    /**
     * Default parameter customizer for parameters with no annotations.
     */
    private BoundCustomizer defaultParameterCustomizer(Parameter parameter, Integer i) {
        final Type parameterType = getParameterType(parameter);
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
                        .createForParameter(sqlObjectType, method, parameter, i, parameterType);
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
        return checkedCreateInstance(sqlStatementCustomizingAnnotation.value());
    }

    @Override
    public Object invoke(HandleSupplier handleSupplier, Object target, Object... args) {
        final Handle h = handleSupplier.getHandle();
        final String locatedSql = locateSql(h);
        final StatementType stmt = createStatement(h, locatedSql);

        // clean the statement when the handle closes
        stmt.attachToHandleForCleanup();

        final SqlObjectStatementConfiguration cfg = stmt.getConfig(SqlObjectStatementConfiguration.class);
        cfg.setArgs(args);
        configureReturner(stmt, cfg);
        applyCustomizers(stmt, safeVarargs(args));
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

    static RowMapper<?> rowMapperFor(UseRowMapper useRowMapper) {
        return checkedCreateInstance(useRowMapper.value());
    }

    static RowReducer<?, ?> rowReducerFor(UseRowReducer useRowReducer) {
        return checkedCreateInstance(useRowReducer.value());
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

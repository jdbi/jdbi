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
package org.jdbi.sqlobject.statement.internal;

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
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jdbi.core.Handle;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.extension.AttachedExtensionHandler;
import org.jdbi.core.extension.ExtensionHandler;
import org.jdbi.core.extension.HandleSupplier;
import org.jdbi.core.generic.GenericTypes;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.result.RowReducer;
import org.jdbi.core.statement.Customizable;
import org.jdbi.core.statement.UnableToExecuteStatementException;
import org.jdbi.sqlobject.SqlObjects;
import org.jdbi.sqlobject.customizer.ConfigMutating;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.sqlobject.customizer.SqlStatementParameterCustomizer;
import org.jdbi.sqlobject.customizer.StatementScoped;
import org.jdbi.sqlobject.statement.ParameterCustomizerFactory;
import org.jdbi.sqlobject.statement.UseRowMapper;
import org.jdbi.sqlobject.statement.UseRowReducer;

import static java.util.stream.Stream.concat;

import static org.jdbi.core.internal.JdbiClassUtils.checkedCreateInstance;
import static org.jdbi.core.internal.JdbiClassUtils.safeVarargs;
import static org.jdbi.core.internal.JdbiClassUtils.superTypes;

/**
 * Base handler for annotations' implementation classes.
 */
abstract class CustomizingStatementHandler implements ExtensionHandler {

    /**
     * When a customizer runs, relative to a statement's lifecycle.
     */
    enum Phase {
        /** Invariant configuration, applied once when a reusable template is built. */
        CONFIGURE,
        /** Per-invocation binding/defining, applied to each execution's statement. */
        BIND,
        /** Per-invocation configuration mutation ({@link ConfigMutating}); forces the classic path. */
        LATE
    }

    private static final Object[] NO_ARGS = new Object[0];

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
                .map(a -> {
                    SqlStatementCustomizerFactory factory = instantiateFactory(a);
                    SqlStatementCustomizer customizer = factory.createForType(a, sqlObjectType);
                    return BoundCustomizer.of(customizer, phaseFor(factory, customizer, Phase.CONFIGURE));
                })
                .forEach(statementCustomizers::add);

        // method customizers
        annotationsFor(method)
                .map(a -> {
                    SqlStatementCustomizerFactory factory = instantiateFactory(a);
                    SqlStatementCustomizer customizer = factory.createForMethod(a, sqlObjectType, method);
                    return BoundCustomizer.of(customizer, phaseFor(factory, customizer, Phase.CONFIGURE));
                })
                .forEach(statementCustomizers::add);

        // parameter customizers
        parameterCustomizers().forEach(statementCustomizers::add);
    }

    /**
     * Determines when a customizer runs. A customizer that (or whose factory) declares
     * {@link ConfigMutating} mutates configuration per invocation and runs {@link Phase#LATE} (classic
     * path); one that declares {@link StatementScoped} operates on the live statement and runs
     * {@link Phase#BIND} even at type/method level; otherwise it runs in the given default phase.
     */
    private static Phase phaseFor(SqlStatementCustomizerFactory factory, Object customizer, Phase defaultPhase) {
        if (factory instanceof ConfigMutating || customizer instanceof ConfigMutating) {
            return Phase.LATE;
        }
        if (factory instanceof StatementScoped || customizer instanceof StatementScoped) {
            return Phase.BIND;
        }
        return defaultPhase;
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
                .<BoundCustomizer>map(a -> {
                    SqlStatementCustomizerFactory factory = instantiateFactory(a);
                    SqlStatementParameterCustomizer c = factory.createForParameter(a, sqlObjectType, method, parameter, i, parameterType);
                    final Phase phase = phaseFor(factory, c, Phase.BIND);
                    return new BoundCustomizer() {
                        @Override
                        public Phase phase() {
                            return phase;
                        }

                        @Override
                        public void warm(ConfigRegistry config) {
                            c.warm(config);
                        }

                        @Override
                        public void apply(Customizable<?> stmt, Object[] args) throws SQLException {
                            c.apply(stmt, args[i]);
                        }
                    };
                })
                .toList();

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
     * Default parameter customizer for parameters with no annotations. Binds the argument, so it runs
     * in the {@link Phase#BIND} phase.
     */
    private BoundCustomizer defaultParameterCustomizer(Parameter parameter, Integer i) {
        final Type parameterType = getParameterType(parameter);
        return new BoundCustomizer() {
            @Override
            public Phase phase() {
                return Phase.BIND;
            }

            @Override
            public void warm(ConfigRegistry config) {
                create(config).warm(config);
            }

            @Override
            public void apply(Customizable<?> stmt, Object[] args) throws SQLException {
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
    public AttachedExtensionHandler attachTo(ConfigRegistry config, Object target) {
        final Supplier<String> locatedSql = locateSql(config);
        final Function<Handle, ? extends Customizable<?>> statementFactory = statementFactory(config, locatedSql);
        return new AttachedExtensionHandler() {
            @Override
            public Object invoke(HandleSupplier handleSupplier, Object... args) throws Exception {
                final Handle h = handleSupplier.getHandle();
                final Customizable<?> stmt = statementFactory.apply(h);

                // clean the statement when the handle closes
                stmt.attachToHandleForCleanup();

                final SqlObjectStatementState state = new SqlObjectStatementState(args);
                stmt.getContext().setExtensionState(state);
                configureReturner(stmt, state);
                applyPerInvocationCustomizers(stmt, safeVarargs(args));
                return state.getReturner().get();
            }

            @Override
            public void warm(ConfigRegistry config) {
                statementCustomizers.forEach(s -> s.warm(config));
                CustomizingStatementHandler.this.warm(config);
            }
        };
    }

    protected void warm(ConfigRegistry config) {}

    /**
     * Builds the per-invocation factory that produces the statement to execute. The default creates a
     * fresh classic statement for each call; handlers that reuse a template override this.
     */
    Function<Handle, ? extends Customizable<?>> statementFactory(ConfigRegistry config, Supplier<String> locatedSql) {
        return handle -> createStatement(handle, locatedSql.get());
    }

    /**
     * Applies the customizers that run per invocation. The default applies every customizer (the
     * classic behavior); handlers that bake configure-phase customizers into a template at build time
     * override this to apply only the {@link Phase#BIND} phase.
     */
    void applyPerInvocationCustomizers(final Customizable<?> stmt, Object[] args) {
        applyCustomizers(stmt, args, null);
    }

    /**
     * Applies bound customizers to the statement. When {@code phase} is null, all customizers apply;
     * otherwise only those in the given phase.
     */
    void applyCustomizers(final Customizable<?> stmt, Object[] args, Phase phase) {
        statementCustomizers.forEach(b -> {
            if (phase != null && b.phase() != phase) {
                return;
            }
            try {
                b.apply(stmt, args);
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException(e, stmt.getContext());
            }
        });
    }

    /**
     * Applies the {@link Phase#CONFIGURE} customizers once, against the given build-time configuration
     * surface. Used by handlers that build a reusable template.
     */
    void applyConfigureCustomizers(final Customizable<?> configSurface) {
        statementCustomizers.forEach(b -> {
            if (b.phase() != Phase.CONFIGURE) {
                return;
            }
            try {
                b.apply(configSurface, NO_ARGS);
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException("Exception thrown configuring statement template", e, null);
            }
        });
    }

    /**
     * @return true if any customizer must mutate configuration per invocation, requiring the classic
     * per-statement path.
     */
    boolean hasLateCustomizers() {
        return statementCustomizers.stream().anyMatch(b -> b.phase() == Phase.LATE);
    }

    abstract void configureReturner(Customizable<?> stmt, SqlObjectStatementState state);

    abstract Customizable<?> createStatement(Handle handle, String locatedSql);

    Supplier<String> locateSql(final ConfigRegistry config) {
        try {
            final String sql = config.get(SqlObjects.class).getSqlLocator().locate(sqlObjectType, method, config);
            return () -> sql;
        } catch (final RuntimeException e) {
            return () -> {
                throw e;
            };
        }
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
     * is ready to apply, together with the phase it runs in.
     */
    private interface BoundCustomizer {

        Phase phase();

        void apply(Customizable<?> stmt, Object[] args) throws SQLException;

        void warm(ConfigRegistry config);

        static BoundCustomizer of(SqlStatementCustomizer inner, Phase phase) {
            return new BoundCustomizer() {
                @Override
                public Phase phase() {
                    return phase;
                }

                @Override
                public void apply(Customizable<?> stmt, Object[] args) throws SQLException {
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

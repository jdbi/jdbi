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
package org.jdbi.core.mapper;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import org.jdbi.core.array.SqlArrayMapperFactory;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.enums.internal.EnumMapperFactory;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.interceptor.JdbiInterceptionChainHolder;
import org.jdbi.core.interceptor.JdbiInterceptor;
import org.jdbi.core.internal.RegistrationLists;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.meta.Alpha;

/**
 * Registry of {@link ColumnMapperFactory} instances. Holds only registration data and the
 * null-primitive coalescing policy; resolving a factory into a {@link ColumnMapper} for a given type
 * (and caching the result) is done per configuration registry by {@link MapperResolver}.
 * <p>
 * This configuration is immutable: {@link #register}, {@link #withInferenceInterceptor} and
 * {@link #coalesceNullPrimitivesToDefaults(boolean)} return a new instance, leaving the receiver unchanged.
 */
public final class ColumnMappers implements JdbiConfig<ColumnMappers> {

    private final JdbiInterceptionChainHolder<ColumnMapper<?>, QualifiedColumnMapperFactory> inferenceInterceptors;

    private final List<QualifiedColumnMapperFactory> factories;

    private final boolean coalesceNullPrimitivesToDefaults;

    public ColumnMappers() {
        this(new JdbiInterceptionChainHolder<>(InferredColumnMapperFactory::new));
    }

    private ColumnMappers(final JdbiInterceptionChainHolder<ColumnMapper<?>, QualifiedColumnMapperFactory> inferenceInterceptors) {
        this(buildDefaultFactories(inferenceInterceptors), inferenceInterceptors, true);
    }

    private ColumnMappers(final List<QualifiedColumnMapperFactory> factories,
            final JdbiInterceptionChainHolder<ColumnMapper<?>, QualifiedColumnMapperFactory> inferenceInterceptors,
            final boolean coalesceNullPrimitivesToDefaults) {
        this.factories = factories;
        this.inferenceInterceptors = inferenceInterceptors;
        this.coalesceNullPrimitivesToDefaults = coalesceNullPrimitivesToDefaults;
    }

    private static List<QualifiedColumnMapperFactory> buildDefaultFactories(
            final JdbiInterceptionChainHolder<ColumnMapper<?>, QualifiedColumnMapperFactory> inferenceInterceptors) {
        // Registration prepends, so the effective consultation order is the reverse of registration order.
        final List<QualifiedColumnMapperFactory> factories = new ArrayList<>();
        prepend(factories, new SqlArrayMapperFactory());
        prepend(factories, new JavaTimeMapperFactory());
        prepend(factories, new SqlTimeMapperFactory());
        prepend(factories, new InternetMapperFactory());
        prepend(factories, new EssentialsMapperFactory());
        prepend(factories, new BoxedMapperFactory());
        prepend(factories, new PrimitiveMapperFactory());
        prepend(factories, new OptionalColumnMapperFactory());
        factories.add(0, new EnumMapperFactory()); // natively a QualifiedColumnMapperFactory; no adaptation needed
        factories.add(0, inferenceInterceptors.process(new NVarcharMapper())); // a ColumnMapper; inferred like register(ColumnMapper)
        return List.copyOf(factories);
    }

    private static void prepend(final List<QualifiedColumnMapperFactory> factories, final ColumnMapperFactory factory) {
        factories.add(0, QualifiedColumnMapperFactory.adapt(factory));
    }

    /**
     * Returns a copy of this configuration with the given interceptor added to the front of the ColumnMapper
     * inference chain, letting it change the standard type inference for {@link #register(ColumnMapper)}.
     *
     * @param interceptor the inference interceptor to add
     * @return the derived configuration
     */
    @CheckReturnValue
    @Alpha
    public ColumnMappers withInferenceInterceptor(final JdbiInterceptor<ColumnMapper<?>, QualifiedColumnMapperFactory> interceptor) {
        final JdbiInterceptionChainHolder<ColumnMapper<?>, QualifiedColumnMapperFactory> newInterceptors = new JdbiInterceptionChainHolder<>(inferenceInterceptors);
        newInterceptors.addFirst(interceptor);
        return new ColumnMappers(factories, newInterceptors, coalesceNullPrimitivesToDefaults);
    }

    /**
     * Register a column mapper which will have its parameterized type inspected to determine what it maps to.
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     * <p>
     * The parameter must be concretely parameterized, we use the type argument T to
     * determine if it applies to a given type.
     *
     * @param mapper the column mapper
     * @return a copy of this configuration with the mapper registered
     * @throws UnsupportedOperationException if the ColumnMapper is not a concretely parameterized type
     */
    @CheckReturnValue
    public ColumnMappers register(ColumnMapper<?> mapper) {
        QualifiedColumnMapperFactory factory = inferenceInterceptors.process(mapper);

        return this.register(factory);
    }

    /**
     * Register a column mapper for a given explicit {@link GenericType}
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param <T> the type
     * @param type the generic type to match with equals.
     * @param mapper the column mapper
     * @return this
     */
    @CheckReturnValue
    public <T> ColumnMappers register(GenericType<T> type, ColumnMapper<T> mapper) {
        return this.register(ColumnMapperFactory.of(type.getType(), mapper));
    }

    /**
     * Register a column mapper for a given explicit {@link Type}
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param type the type to match with equals.
     * @param mapper the column mapper
     * @return this
     */
    @CheckReturnValue
    public ColumnMappers register(Type type, ColumnMapper<?> mapper) {
        return this.register(ColumnMapperFactory.of(type, mapper));
    }

    /**
     * Register a column mapper for a given {@link QualifiedType}
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param type the type to match with equals.
     * @param mapper the column mapper
     * @return this
     */
    @CheckReturnValue
    public <T> ColumnMappers register(QualifiedType<T> type, ColumnMapper<T> mapper) {
        return this.register(QualifiedColumnMapperFactory.of(type, mapper));
    }

    /**
     * Register a column mapper factory.
     * <p>
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param factory the column mapper factory
     * @return a copy of this configuration with the factory registered
     */
    @CheckReturnValue
    public ColumnMappers register(ColumnMapperFactory factory) {
        return register(QualifiedColumnMapperFactory.adapt(factory));
    }

    /**
     * Register a qualified column mapper factory.
     * <p>
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param factory the qualified column mapper factory
     * @return a copy of this configuration with the factory registered
     */
    @CheckReturnValue
    public ColumnMappers register(QualifiedColumnMapperFactory factory) {
        return new ColumnMappers(RegistrationLists.prepend(factories, factory), inferenceInterceptors, coalesceNullPrimitivesToDefaults);
    }

    /**
     * Registers all of the given column mapper factories in a single derivation, as if each were passed to
     * {@link #register(ColumnMapperFactory)} in iteration order (so the last factory in the collection has the
     * highest priority). More efficient and readable than chaining individual {@code register} calls.
     *
     * @param factories the column mapper factories to add
     * @return a copy of this configuration with the factories registered
     */
    @CheckReturnValue
    public ColumnMappers register(Collection<? extends ColumnMapperFactory> factories) {
        if (factories.isEmpty()) {
            return this;
        }
        return new ColumnMappers(RegistrationLists.prependAll(this.factories, factories, QualifiedColumnMapperFactory::adapt),
                inferenceInterceptors, coalesceNullPrimitivesToDefaults);
    }

    /**
     * Returns the registered factories, most-recently-registered first. Consumed by {@link MapperResolver}.
     *
     * @return the registered column mapper factories
     */
    List<QualifiedColumnMapperFactory> getFactories() {
        return factories;
    }

    /**
     * Returns true if database {@code NULL} values should be transformed to the default value for primitives.
     *
     * @return {@code true} if database {@code NULL}s should translate to the JDBC defaults for primitives, or throw an exception otherwise.
     *
     * Default value is true: nulls will be coalesced to defaults.
     */
    public boolean getCoalesceNullPrimitivesToDefaults() {
        return coalesceNullPrimitivesToDefaults;
    }

    /**
     * Use the JDBC default value for primitive types if a SQL NULL value was returned by the database.
     * If this property is set to {@code false}, Jdbi will throw an exception when trying to map a SQL {@code NULL} value to a primitive type.
     *
     * @param coalesceNullPrimitivesToDefaults If true, then use the JDBC default value, otherwise throw an exception.
     * @return a copy of this configuration with the policy set
     */
    @CheckReturnValue
    public ColumnMappers coalesceNullPrimitivesToDefaults(boolean coalesceNullPrimitivesToDefaults) {
        return new ColumnMappers(factories, inferenceInterceptors, coalesceNullPrimitivesToDefaults);
    }

    @Override
    public ColumnMappers createCopy() {
        // Immutable: safe to share across registries.
        return this;
    }
}

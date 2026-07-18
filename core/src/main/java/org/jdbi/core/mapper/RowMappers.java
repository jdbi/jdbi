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
import java.util.function.Function;

import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.interceptor.JdbiInterceptionChainHolder;
import org.jdbi.core.interceptor.JdbiInterceptor;
import org.jdbi.core.internal.RegistrationLists;
import org.jdbi.core.mapper.reflect.internal.PojoMapperFactory;
import org.jdbi.core.statement.Query;
import org.jdbi.meta.Alpha;

/**
 * Registry of {@link RowMapperFactory} instances. Holds only registration data; resolving a factory
 * into a {@link RowMapper} for a given type (and caching the result) is done per configuration registry
 * by {@link MapperResolver}.
 * <p>
 * This configuration is immutable: {@link #register} and {@link #withInferenceInterceptor} return a new
 * instance, leaving the receiver unchanged.
 */
public final class RowMappers implements JdbiConfig<RowMappers> {

    private final JdbiInterceptionChainHolder<RowMapper<?>, RowMapperFactory> inferenceInterceptors;

    private final List<RowMapperFactory> factories;

    public RowMappers() {
        this(buildDefaultFactories(), new JdbiInterceptionChainHolder<>(InferredRowMapperFactory::new));
    }

    private RowMappers(final List<RowMapperFactory> factories,
            final JdbiInterceptionChainHolder<RowMapper<?>, RowMapperFactory> inferenceInterceptors) {
        this.factories = factories;
        this.inferenceInterceptors = inferenceInterceptors;
    }

    private static List<RowMapperFactory> buildDefaultFactories() {
        // Registration prepends, so the effective consultation order is the reverse of registration order.
        final List<RowMapperFactory> factories = new ArrayList<>();
        factories.add(0, MapEntryMapper.factory());
        factories.add(0, new PojoMapperFactory());
        factories.add(0, new OptionalRowMapperFactory());
        return List.copyOf(factories);
    }

    /**
     * Returns a copy of this configuration with the given interceptor added to the front of the RowMapper
     * inference chain, letting it change the standard type inference for {@link #register(RowMapper)}.
     *
     * @param interceptor the inference interceptor to add
     * @return the derived configuration
     */
    @Alpha
    public RowMappers withInferenceInterceptor(final JdbiInterceptor<RowMapper<?>, RowMapperFactory> interceptor) {
        final JdbiInterceptionChainHolder<RowMapper<?>, RowMapperFactory> newInterceptors = new JdbiInterceptionChainHolder<>(inferenceInterceptors);
        newInterceptors.addFirst(interceptor);
        return new RowMappers(factories, newInterceptors);
    }

    /**
     * Register a row mapper which will have its parameterized type inspected to determine what it maps to.
     * Will be used with {@link Query#mapTo(Class)} for registered mappings.
     * <p>
     * The parameter must be concretely parameterized, we use the type argument T to
     * determine if it applies to a given type.
     * <p>
     * {@link java.lang.Object} is not supported as a concrete parameter type.
     *
     * @param mapper the row mapper
     * @return a copy of this configuration with the mapper registered
     * @throws UnsupportedOperationException if the RowMapper is not a concretely parameterized type
     */
    public RowMappers register(RowMapper<?> mapper) {
        RowMapperFactory factory = inferenceInterceptors.process(mapper);

        return this.register(factory);
    }

    /**
     * Register a row mapper for a given type.
     *
     * @param <T> the type
     * @param type the type to match with equals.
     * @param mapper the row mapper
     * @return this
     */
    public <T> RowMappers register(GenericType<T> type, RowMapper<T> mapper) {
        return this.register(RowMapperFactory.of(type.getType(), mapper));
    }

    /**
     * Register a row mapper for a given type.
     *
     * @param type the type to match with equals.
     * @param mapper the row mapper
     * @return this
     */
    public RowMappers register(Type type, RowMapper<?> mapper) {
        return this.register(RowMapperFactory.of(type, mapper));
    }

    /**
     * Register a row mapper factory.
     * <p>
     * Will be used with {@link Query#mapTo(Class)} for registered mappings.
     *
     * @param factory the row mapper factory
     * @return a copy of this configuration with the factory registered
     */
    public RowMappers register(RowMapperFactory factory) {
        return new RowMappers(RegistrationLists.prepend(factories, factory), inferenceInterceptors);
    }

    /**
     * Registers all of the given row mapper factories in a single derivation, as if each were passed to
     * {@link #register(RowMapperFactory)} in iteration order (so the last factory in the collection has the
     * highest priority). More efficient and readable than chaining individual {@code register} calls.
     *
     * @param factories the row mapper factories to add
     * @return a copy of this configuration with the factories registered
     */
    public RowMappers register(Collection<? extends RowMapperFactory> factories) {
        if (factories.isEmpty()) {
            return this;
        }
        return new RowMappers(RegistrationLists.prependAll(this.factories, factories, Function.identity()), inferenceInterceptors);
    }

    /**
     * Returns the registered factories, most-recently-registered first. Consumed by {@link MapperResolver}.
     *
     * @return the registered row mapper factories
     */
    List<RowMapperFactory> getFactories() {
        return factories;
    }

    @Override
    public RowMappers createCopy() {
        // Immutable: safe to share across registries.
        return this;
    }
}

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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.interceptor.JdbiInterceptionChainHolder;
import org.jdbi.core.mapper.reflect.internal.PojoMapperFactory;
import org.jdbi.core.statement.Query;
import org.jdbi.meta.Alpha;

/**
 * Registry of {@link RowMapperFactory} instances. Holds only registration data; resolving a factory
 * into a {@link RowMapper} for a given type (and caching the result) is done per configuration registry
 * by {@link MapperResolver}.
 */
public class RowMappers implements JdbiConfig<RowMappers> {

    private final JdbiInterceptionChainHolder<RowMapper<?>, RowMapperFactory> inferenceInterceptors;

    private final List<RowMapperFactory> factories;

    public RowMappers() {
        inferenceInterceptors = new JdbiInterceptionChainHolder<>(InferredRowMapperFactory::new);
        factories = new CopyOnWriteArrayList<>();
        register(MapEntryMapper.factory());
        register(new PojoMapperFactory());
        register(new OptionalRowMapperFactory());
    }

    private RowMappers(RowMappers that) {
        factories = new CopyOnWriteArrayList<>(that.factories);
        inferenceInterceptors = new JdbiInterceptionChainHolder<>(that.inferenceInterceptors);
    }

    /**
     * Returns the {@link JdbiInterceptionChainHolder} for the RowMapper inference. This chain allows registration of custom interceptors to change the standard type
     * inference for the {@link RowMappers#register(RowMapper)} method.
     */
    @Alpha
    public JdbiInterceptionChainHolder<RowMapper<?>, RowMapperFactory> getInferenceInterceptors() {
        return inferenceInterceptors;
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
     * @return this
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
     * @return this
     */
    public RowMappers register(RowMapperFactory factory) {
        factories.add(0, factory);
        return this;
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
        return new RowMappers(this);
    }
}

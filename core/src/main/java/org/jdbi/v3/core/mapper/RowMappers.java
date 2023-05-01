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
package org.jdbi.v3.core.mapper;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.interceptor.JdbiInterceptionChainHolder;
import org.jdbi.v3.core.internal.CopyOnWriteHashMap;
import org.jdbi.v3.core.internal.JdbiOptionals;
import org.jdbi.v3.core.mapper.reflect.internal.PojoMapperFactory;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.meta.Alpha;

/**
 * Configuration registry for {@link RowMapperFactory} instances.
 */
public class RowMappers implements JdbiConfig<RowMappers> {

    private final JdbiInterceptionChainHolder<RowMapper<?>, RowMapperFactory> inferenceInterceptors;

    private final List<RowMapperFactory> factories;
    private final Map<Type, Optional<RowMapper<?>>> cache;

    private ConfigRegistry registry;

    public RowMappers() {
        inferenceInterceptors = new JdbiInterceptionChainHolder<>(InferredRowMapperFactory::new);
        factories = new CopyOnWriteArrayList<>();
        cache = new CopyOnWriteHashMap<>();
        register(MapEntryMapper.factory());
        register(new PojoMapperFactory());
    }

    private RowMappers(RowMappers that) {
        factories = new CopyOnWriteArrayList<>(that.factories);
        cache = new CopyOnWriteHashMap<>(that.cache);
        inferenceInterceptors = new JdbiInterceptionChainHolder<>(that.inferenceInterceptors);
    }

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
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
        cache.clear();
        return this;
    }

    /**
     * Obtain a row mapper for the given type in the given context.
     *
     * @param <T> the type of the mapper to find
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<RowMapper<T>> findFor(Class<T> type) {
        RowMapper<T> mapper = (RowMapper<T>) findFor((Type) type).orElse(null);
        return Optional.ofNullable(mapper);
    }

    /**
     * Obtain a row mapper for the given type in the given context.
     *
     * @param <T> the type of the mapper to find
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<RowMapper<T>> findFor(GenericType<T> type) {
        RowMapper<T> mapper = (RowMapper<T>) findFor(type.getType()).orElse(null);
        return Optional.ofNullable(mapper);
    }

    /**
     * Obtain a row mapper for the given type in the given context.
     *
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    public Optional<RowMapper<?>> findFor(Type type) {
        // ConcurrentHashMap can enter an infinite loop on nested computeIfAbsent calls.
        // Since row mappers can decorate other row mappers, we have to populate the cache the old fashioned way.
        // See https://bugs.openjdk.java.net/browse/JDK-8062841, https://bugs.openjdk.java.net/browse/JDK-8142175
        Optional<RowMapper<?>> cached = cache.get(type);

        if (cached != null) {
            return cached;
        }

        Optional<RowMapper<?>> mapper = factories.stream()
                .flatMap(factory -> JdbiOptionals.stream(factory.build(type, registry)))
                .findFirst();

        mapper.ifPresent(m -> m.init(registry));

        cache.put(type, mapper);

        return mapper;
    }

    @Override
    public RowMappers createCopy() {
        return new RowMappers(this);
    }
}

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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.core.config.ConfigView;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.internal.CopyOnWriteHashMap;
import org.jdbi.core.qualifier.QualifiedType;

/**
 * Resolves row and column mappers for a specific {@link ConfigView}, caching the results.
 * <p>
 * A resolver reads the registered factories from the registry's {@link RowMappers} and
 * {@link ColumnMappers} (which hold only registration data) and turns them into mappers, memoizing the
 * outcome. It is obtained per registry via {@link #forRegistry(ConfigView)} and is scoped to that
 * registry: because it is never shared across registry copies, it safely holds the registry reference,
 * and its cache is warm across the many statements executed against a shared registry, yet a forked
 * registry starts with an empty cache and re-resolves against its own factories.
 */
public final class MapperResolver {

    /**
     * Returns the mapper resolver for the given registry, creating it on first use.
     *
     * @param config the configuration registry to resolve against
     * @return the registry's memoized mapper resolver
     */
    public static MapperResolver forRegistry(final ConfigView config) {
        return config.readAs(MapperResolver.class, MapperResolver::new);
    }

    private final ConfigView registry;
    private final Map<Type, Optional<RowMapper<?>>> rowCache = new CopyOnWriteHashMap<>();
    private final Map<QualifiedType<?>, Optional<? extends ColumnMapper<?>>> columnCache = new CopyOnWriteHashMap<>();

    // Registration only ever adds factories, so a change in factory count means a mapper was registered
    // on this (still-mutable) registry after we cached; drop the stale cache. Once registration forks the
    // registry (immutable-config step), each fork has its own fresh resolver and this guard is moot.
    private volatile int rowFactoryCount = -1;
    private volatile int columnFactoryCount = -1;

    private MapperResolver(final ConfigView registry) {
        this.registry = registry;
    }

    /**
     * Obtain a mapper for the given type: a registered row mapper if present, otherwise a registered
     * column mapper adapted to map the first column, otherwise empty.
     *
     * @param <T>  the mapped type
     * @param type the target type
     * @return the mapper, or empty if none is registered
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<RowMapper<T>> findMapper(final Class<T> type) {
        return (Optional<RowMapper<T>>) (Optional<?>) findMapper((Type) type);
    }

    /**
     * Obtain a mapper for the given type. See {@link #findMapper(Class)}.
     *
     * @param <T>  the mapped type
     * @param type the target type
     * @return the mapper, or empty if none is registered
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<RowMapper<T>> findMapper(final GenericType<T> type) {
        return (Optional<RowMapper<T>>) (Optional<?>) findMapper(type.getType());
    }

    /**
     * Obtain a mapper for the given type. See {@link #findMapper(Class)}.
     *
     * @param type the target type
     * @return the mapper, or empty if none is registered
     */
    public Optional<RowMapper<?>> findMapper(final Type type) {
        return findMapper(QualifiedType.of(type)).map(Function.identity());
    }

    /**
     * Obtain a mapper for the given qualified type. If unqualified and a row mapper is registered, it is
     * returned; otherwise a registered column mapper is adapted to map the first column.
     *
     * @param <T>  the mapped type
     * @param type the target qualified type
     * @return the mapper, or empty if none is registered
     */
    public <T> Optional<RowMapper<T>> findMapper(final QualifiedType<T> type) {
        if (type.getQualifiers().isEmpty()) {
            @SuppressWarnings("unchecked")
            final Optional<RowMapper<T>> result = findRowMapper(type.getType()).map(m -> (RowMapper<T>) m);
            if (result.isPresent()) {
                return result;
            }
        }
        return findColumnMapper(type).map(SingleColumnMapper::new);
    }

    /**
     * Obtain a row mapper for the given type.
     *
     * @param <T>  the mapped type
     * @param type the target type
     * @return the row mapper, or empty if none is registered
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<RowMapper<T>> findRowMapper(final Class<T> type) {
        return (Optional<RowMapper<T>>) (Optional<?>) findRowMapper((Type) type);
    }

    /**
     * Obtain a row mapper for the given type.
     *
     * @param <T>  the mapped type
     * @param type the target type
     * @return the row mapper, or empty if none is registered
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<RowMapper<T>> findRowMapper(final GenericType<T> type) {
        return (Optional<RowMapper<T>>) (Optional<?>) findRowMapper(type.getType());
    }

    /**
     * Obtain a row mapper for the given type.
     *
     * @param type the target type
     * @return the row mapper, or empty if none is registered
     */
    public Optional<RowMapper<?>> findRowMapper(final Type type) {
        // ConcurrentHashMap can enter an infinite loop on nested computeIfAbsent calls.
        // Since row mappers can decorate other row mappers, we have to populate the cache the old fashioned way.
        // See https://bugs.openjdk.java.net/browse/JDK-8062841, https://bugs.openjdk.java.net/browse/JDK-8142175
        final List<RowMapperFactory> factories = registry.get(RowMappers.class).getFactories();
        if (factories.size() != rowFactoryCount) {
            rowCache.clear();
            rowFactoryCount = factories.size();
        }

        final Optional<RowMapper<?>> cached = rowCache.get(type);
        if (cached != null) {
            return cached;
        }

        for (final RowMapperFactory factory : factories) {
            final Optional<RowMapper<?>> maybeMapper = factory.build(type, registry);
            final RowMapper<?> mapper = maybeMapper.orElse(null);
            if (mapper != null) {
                mapper.init(registry);
                rowCache.put(type, maybeMapper);
                return maybeMapper;
            }
        }

        rowCache.put(type, Optional.empty());
        return Optional.empty();
    }

    /**
     * Obtain a column mapper for the given type.
     *
     * @param <T>  the mapped type
     * @param type the target type
     * @return the column mapper, or empty if none is registered
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<ColumnMapper<T>> findColumnMapper(final Class<T> type) {
        return (Optional<ColumnMapper<T>>) (Optional<?>) findColumnMapper((Type) type);
    }

    /**
     * Obtain a column mapper for the given type.
     *
     * @param <T>  the mapped type
     * @param type the target type
     * @return the column mapper, or empty if none is registered
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<ColumnMapper<T>> findColumnMapper(final GenericType<T> type) {
        return (Optional<ColumnMapper<T>>) (Optional<?>) findColumnMapper(type.getType());
    }

    /**
     * Obtain a column mapper for the given type.
     *
     * @param type the target type
     * @return the column mapper, or empty if none is registered
     */
    public Optional<ColumnMapper<?>> findColumnMapper(final Type type) {
        return findColumnMapper(QualifiedType.of(type)).map(Function.identity());
    }

    /**
     * Obtain a column mapper for the given qualified type.
     *
     * @param <T>  the mapped type
     * @param type the target qualified type
     * @return the column mapper, or empty if none is registered
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<ColumnMapper<T>> findColumnMapper(final QualifiedType<T> type) {
        // Populate the cache the old fashioned way; see the note in findRowMapper.
        final List<QualifiedColumnMapperFactory> factories = registry.get(ColumnMappers.class).getFactories();
        if (factories.size() != columnFactoryCount) {
            columnCache.clear();
            columnFactoryCount = factories.size();
        }

        final Optional<ColumnMapper<T>> cached = (Optional<ColumnMapper<T>>) (Optional<?>) columnCache.get(type);
        if (cached != null) {
            return cached;
        }

        for (final QualifiedColumnMapperFactory factory : factories) {
            final Optional<ColumnMapper<T>> maybeMapper = (Optional<ColumnMapper<T>>) (Optional<?>) factory.build(type, registry);
            final ColumnMapper<T> mapper = maybeMapper.orElse(null);
            if (mapper != null) {
                mapper.init(registry);
                columnCache.put(type, maybeMapper);
                return maybeMapper;
            }
        }

        columnCache.put(type, Optional.empty());
        return Optional.empty();
    }
}

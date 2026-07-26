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
package org.jdbi.core.collector;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;

import org.jdbi.core.config.ConfigView;
import org.jdbi.core.internal.CopyOnWriteHashMap;

/**
 * Resolves collectors for a specific {@link ConfigView}, caching the results.
 * <p>
 * A resolver reads the registered factories from the registry's {@link JdbiCollectors} (which holds only
 * registration data) and turns a container type into a {@link Collector}, memoizing the outcome. It is
 * obtained per registry via {@link #forRegistry(ConfigView)} and is scoped to that registry: its cache
 * is warm across the many statements executed against a shared registry, yet a forked registry starts with
 * an empty cache and re-resolves against its own factories.
 */
public final class CollectorResolver {

    /**
     * Returns the collector resolver for the given registry, creating it on first use.
     *
     * @param config the configuration registry to resolve against
     * @return the registry's memoized collector resolver
     */
    public static CollectorResolver forRegistry(final ConfigView config) {
        return config.readAs(CollectorResolver.class, CollectorResolver::new);
    }

    private final ConfigView registry;
    private final Map<Type, Optional<CollectorFactory>> factoryCache = new CopyOnWriteHashMap<>();

    // Registration only ever adds factories, so a change in factory count means a factory was registered
    // on this (still-mutable) registry after we cached; drop the stale cache. Once registration forks the
    // registry (immutable-config step), each fork has its own fresh resolver and this guard is moot.
    private volatile int factoryCount = -1;

    private CollectorResolver(final ConfigView registry) {
        this.registry = registry;
    }

    /**
     * Obtain a collector for the given container type.
     *
     * @param containerType the container type.
     * @return a Collector for the given container type, or empty if no collector is registered for the given type.
     */
    public Optional<Collector<?, ?, ?>> findFor(final Type containerType) {
        return findFactoryFor(containerType)
                .map(f -> f.build(containerType));
    }

    /**
     * Returns the element type for the given container type.
     *
     * @param containerType the container type.
     * @return the element type for the given container type, if available.
     */
    public Optional<Type> findElementTypeFor(final Type containerType) {
        return findFactoryFor(containerType)
                .flatMap(f -> f.elementType(containerType));
    }

    private Optional<CollectorFactory> findFactoryFor(final Type containerType) {
        final List<CollectorFactory> factories = registry.get(JdbiCollectors.class).getFactories();
        if (factories.size() != factoryCount) {
            factoryCache.clear();
            factoryCount = factories.size();
        }

        final Optional<CollectorFactory> cached = factoryCache.get(containerType);
        if (cached != null) {
            return cached;
        }

        final Optional<CollectorFactory> entry = factories.stream()
                .filter(f -> f.accepts(containerType))
                .findFirst();
        factoryCache.putIfAbsent(containerType, entry);
        return entry;
    }
}

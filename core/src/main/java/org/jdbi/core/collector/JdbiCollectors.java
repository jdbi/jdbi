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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.internal.RegistrationLists;

/**
 * Registry of collector factories. Holds only registration data; resolving a factory into a
 * {@link Collector} for a given container type (and caching the result) is done per configuration registry
 * by {@link CollectorResolver}.
 * <p>
 * This configuration is immutable: the {@code register} methods return a new instance, leaving the receiver
 * unchanged.
 */
public final class JdbiCollectors implements JdbiConfig<JdbiCollectors> {

    private static final List<CollectorFactory> DEFAULT_FACTORIES = buildDefaultFactories();

    private final List<CollectorFactory> factories;

    public JdbiCollectors() {
        this(DEFAULT_FACTORIES);
    }

    private JdbiCollectors(final List<CollectorFactory> factories) {
        this.factories = factories;
    }

    private static List<CollectorFactory> buildDefaultFactories() {
        // Registration prepends, so the effective consultation order is the reverse of registration order.
        final List<CollectorFactory> f = new ArrayList<>();
        f.add(0, new MapCollectorFactory());
        f.add(0, new OptionalCollectorFactory());
        f.add(0, new ListCollectorFactory());
        f.add(0, new SetCollectorFactory());
        f.add(0, new OptionalPrimitiveCollectorFactory());
        f.add(0, new ArrayCollectorFactory());
        f.add(0, new EnumSetCollectorFactory());
        return List.copyOf(f);
    }

    /**
     * Register a new {@link CollectorFactory}.
     * @param factory A collector factory
     * @return a copy of this configuration with the factory registered
     */
    @CheckReturnValue
    public JdbiCollectors register(final CollectorFactory factory) {
        return new JdbiCollectors(RegistrationLists.prepend(factories, factory));
    }

    /**
     * Register a new {@link Collector} for the given type.
     * @param collectionType The type that this collector will return
     * @param collector A {@link Collector} implementation
     * @return a copy of this configuration with the collector registered
     * @since 3.38.0
     * @see org.jdbi.core.config.Configurable#registerCollector(CollectorFactory)
     */
    @CheckReturnValue
    public JdbiCollectors registerCollector(final Type collectionType, final Collector<?, ?, ?> collector) {
        return register(CollectorFactory.collectorFactory(collectionType, collector));
    }

    /**
     * Returns the registered factories, most-recently-registered first. Consumed by {@link CollectorResolver}.
     *
     * @return the registered collector factories
     */
    List<CollectorFactory> getFactories() {
        return factories;
    }

    @Override
    public JdbiCollectors createCopy() {
        // Immutable: safe to share across registries.
        return this;
    }
}

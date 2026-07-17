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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collector;

import org.jdbi.core.config.JdbiConfig;

/**
 * Registry of collector factories. Holds only registration data; resolving a factory into a
 * {@link Collector} for a given container type (and caching the result) is done per configuration registry
 * by {@link CollectorResolver}.
 * Contains a set of collector factories, registered by the application.
 */
public class JdbiCollectors implements JdbiConfig<JdbiCollectors> {
    private final List<CollectorFactory> factories;

    public JdbiCollectors() {
        factories = new CopyOnWriteArrayList<>();
        register(new MapCollectorFactory());
        register(new OptionalCollectorFactory());
        register(new ListCollectorFactory());
        register(new SetCollectorFactory());
        register(new OptionalPrimitiveCollectorFactory());
        register(new ArrayCollectorFactory());
        register(new EnumSetCollectorFactory());
    }

    private JdbiCollectors(JdbiCollectors that) {
        factories = new CopyOnWriteArrayList<>(that.factories);
    }

    /**
     * Register a new {@link CollectorFactory}.
     * @param factory A collector factory
     * @return this
     */
    public JdbiCollectors register(CollectorFactory factory) {
        factories.add(0, factory);
        return this;
    }

    /**
     * Register a new {@link Collector} for the given type.
     * @param collectionType The type that this collector will return
     * @param collector A {@link Collector} implementation
     * @return this
     * @since 3.38.0
     * @see org.jdbi.core.config.Configurable#registerCollector(CollectorFactory)
     */
    public JdbiCollectors registerCollector(Type collectionType, Collector<?, ?, ?> collector) {
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
        return new JdbiCollectors(this);
    }
}

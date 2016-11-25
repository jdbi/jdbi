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
package org.jdbi.v3.core.collector;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collector;

import org.jdbi.v3.core.JdbiConfig;

/**
 * Registry of collector factories.
 * Contains a set of collector factories, registered by the application.
 */
public class JdbiCollectors implements JdbiConfig<JdbiCollectors> {
    private final List<CollectorFactory> factories = new CopyOnWriteArrayList<>();

    public JdbiCollectors() {
        factories.addAll(BuiltInCollectorFactories.get());
    }

    private JdbiCollectors(JdbiCollectors that) {
        factories.addAll(that.factories);
    }

    public JdbiCollectors register(CollectorFactory factory) {
        factories.add(0, factory);
        return this;
    }

    /**
     * Obtain a collector for the given type.
     *
     * @param containerType the container type.
     * @return a Collector for the given container type, or empty null if no collector is registered for the given type.
     */
    public Optional<Collector<?, ?, ?>> findFor(Type containerType) {
        return findFactoryFor(containerType)
                .map(f -> f.build(containerType));
    }

    /**
     * Returns the element type for the given container type.
     *
     * @param containerType the container type.
     * @return the element type for the given container type, if available.
     */
    public Optional<Type> findElementTypeFor(Type containerType) {
        return findFactoryFor(containerType)
                .flatMap(f -> f.elementType(containerType));
    }

    private Optional<CollectorFactory> findFactoryFor(Type containerType) {
        return factories.stream()
                .filter(f -> f.accepts(containerType))
                .findFirst();
    }

    @Override
    public JdbiCollectors createCopy() {
        return new JdbiCollectors(this);
    }
}

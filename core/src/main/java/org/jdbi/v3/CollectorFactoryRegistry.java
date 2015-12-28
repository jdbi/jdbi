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
package org.jdbi.v3;

import static org.jdbi.v3.Types.getErasedType;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.jdbi.v3.tweak.CollectorFactory;

/**
 * Registry of collector factories.
 * Contains a set of collector factories, registered by the application.
 */
class CollectorFactoryRegistry {

    private final Set<CollectorFactory> factories = new CopyOnWriteArraySet<>();

    CollectorFactoryRegistry() {
        factories.add(new ListCollectorFactory<>());
        factories.add(new SortedSetCollectorFactory<>());
        factories.add(new SetCollectorFactory<>());
    }

    CollectorFactoryRegistry createChild() {
        return copyOf(this);
    }

    void register(CollectorFactory factory) {
        factories.add(factory);
    }

    Collector<?, ?, ?> createCollectorFor(Type type) {
        for (CollectorFactory factory : factories) {
            if (factory.accepts(type)) {
                return factory.newCollector(type);
            }
        }

        throw new IllegalStateException("No collector builder available for " + type);
    }

    static CollectorFactoryRegistry copyOf(CollectorFactoryRegistry registry) {
        CollectorFactoryRegistry newRegistry = new CollectorFactoryRegistry();
        newRegistry.factories.addAll(registry.factories);
        return newRegistry;
    }

    private static class SortedSetCollectorFactory<T> implements CollectorFactory {

        @Override
        public boolean accepts(Type type) {
            return getErasedType(type).equals(SortedSet.class);
        }

        @Override
        public Collector<T, ?, SortedSet<T>> newCollector(Type type) {
            return Collectors.toCollection(TreeSet::new);
        }
    }

    private static class ListCollectorFactory<T> implements CollectorFactory {

        @Override
        public boolean accepts(Type type) {
            return getErasedType(type).equals(List.class);
        }

        @Override
        public Collector<T, ?, List<T>> newCollector(Type type) {
            return Collectors.toList();
        }
    }

    private static class SetCollectorFactory<T> implements CollectorFactory {

        @Override
        public boolean accepts(Type type) {
            return getErasedType(type).equals(Set.class);
        }

        @Override
        public Collector<T, ?, Set<T>> newCollector(Type type) {
            return Collectors.toSet();
        }
    }
}

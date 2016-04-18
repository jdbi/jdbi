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

import static org.jdbi.v3.Types.findGenericParameter;
import static org.jdbi.v3.Types.getErasedType;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.jdbi.v3.tweak.CollectorFactory;

/**
 * Registry of collector factories.
 * Contains a set of collector factories, registered by the application.
 */
class CollectorFactoryRegistry {

    private final List<CollectorFactory> factories = new CopyOnWriteArrayList<>();

    CollectorFactoryRegistry() {
        factories.add(new ListCollectorFactory());
        factories.add(new SortedSetCollectorFactory());
        factories.add(new SetCollectorFactory());
        factories.add(new OptionalCollectorFactory());
    }

    private CollectorFactoryRegistry(CollectorFactoryRegistry that) {
        factories.addAll(that.factories);
    }

    void register(CollectorFactory factory) {
        factories.add(0, factory);
    }

    Optional<Collector<?, ?, ?>> findCollectorFor(Type containerType) {
        return findFactoryFor(containerType)
                .map(f -> f.build(containerType));
    }

    Optional<Type> elementTypeFor(Type containerType) {
        return findFactoryFor(containerType)
                .flatMap(f -> f.elementType(containerType));
    }

    private Optional<CollectorFactory> findFactoryFor(Type containerType) {
        return factories.stream()
                .filter(f -> f.accepts(containerType))
                .findFirst();
    }

    static CollectorFactoryRegistry copyOf(CollectorFactoryRegistry registry) {
        return new CollectorFactoryRegistry(registry);
    }

    private static class SortedSetCollectorFactory implements CollectorFactory {
        @Override
        public boolean accepts(Type containerType) {
            return getErasedType(containerType) == SortedSet.class;
        }

        @Override
        public Optional<Type> elementType(Type containerType) {
            return findGenericParameter(containerType, SortedSet.class);
        }

        @Override
        public Collector<?, ?, ?> build(Type containerType) {
            return Collectors.toCollection(TreeSet::new);
        }
    }

    private static class ListCollectorFactory implements CollectorFactory {
        @Override
        public boolean accepts(Type containerType) {
            return getErasedType(containerType) == List.class;
        }

        @Override
        public Optional<Type> elementType(Type containerType) {
            return findGenericParameter(containerType, List.class);
        }

        @Override
        public Collector<?, ?, ?> build(Type containerType) {
            return Collectors.toList();
        }
    }

    private static class SetCollectorFactory implements CollectorFactory {
        @Override
        public boolean accepts(Type containerType) {
            return getErasedType(containerType) == Set.class;
        }

        @Override
        public Optional<Type> elementType(Type containerType) {
            return findGenericParameter(containerType, Set.class);
        }

        @Override
        public Collector<?, ?, ?> build(Type containerType) {
            return Collectors.toSet();
        }
    }

    private static class OptionalCollectorFactory implements CollectorFactory {
        @Override
        public boolean accepts(Type containerType) {
            return getErasedType(containerType) == Optional.class;
        }

        @Override
        public Optional<Type> elementType(Type containerType) {
            return findGenericParameter(containerType, Optional.class);
        }

        @Override
        public Collector<?, ?, ?> build(Type containerType) {
            return Collector.<Object, OptionalBuilder, Optional<Object>>of(
                    OptionalBuilder::new,
                    OptionalBuilder::set,
                    (left, right) -> left.build().isPresent() ? left : right,
                    OptionalBuilder::build);
        }

    }

    private static class OptionalBuilder<T> {
        private Optional<T> optional = Optional.empty();

        public void set(T value) {
            optional = Optional.of(value);
        }

        public Optional<T> build() {
            return optional;
        }
    }
}

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
package org.jdbi.v3.guava;

import static org.jdbi.v3.Types.getErasedType;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collector;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

import org.jdbi.v3.tweak.CollectorFactory;

/**
 * Provide Collector instances that create Guava collection types, especially immutable
 * versions of the base Java collections.
 */
public class GuavaCollectors {
    /**
     * @return a collector into {@code ImmutableList<T>}
     */
    public static <T> Collector<T, ?, ImmutableList<T>> toImmutableList() {
        return Collector.<T, ImmutableList.Builder<T>, ImmutableList<T>>of(
                ImmutableList::builder,
                ImmutableList.Builder::add,
                GuavaCollectors::combineBuilders,
                ImmutableList.Builder::build);
    }

    /**
     * @return a collector into {@code ImmutableSet<T>}
     */
    public static <T> Collector<T, ?, ImmutableSet<T>> toImmutableSet() {
        return Collector.<T, ImmutableSet.Builder<T>, ImmutableSet<T>>of(
                ImmutableSet::builder,
                ImmutableSet.Builder::add,
                GuavaCollectors::combineBuilders,
                ImmutableSet.Builder::build);
    }

    /**
     * @return a collector into {@code ImmutableSortedSet<T>}
     */
    public static <T extends Comparable<T>> Collector<T, ?, ImmutableSortedSet<T>> toImmutableSortedSet() {
        return Collector.<T, ImmutableSortedSet.Builder<T>, ImmutableSortedSet<T>>of(
                ImmutableSortedSet::naturalOrder,
                ImmutableSortedSet.Builder::add,
                GuavaCollectors::combineBuilders,
                ImmutableSortedSet.Builder::build);
    }

    /**
     * @param comparator the comparator for sorting set elements.
     * @return a collector into {@code ImmutableSortedSet<T>} using the given comparator for sorting
     */
    public static <T> Collector<T, ?, ImmutableSortedSet<T>> toImmutableSortedSet(Comparator<T> comparator) {
        return Collector.<T, ImmutableSortedSet.Builder<T>, ImmutableSortedSet<T>> of(
                () -> ImmutableSortedSet.orderedBy(comparator),
                ImmutableSortedSet.Builder::add,
                GuavaCollectors::combineBuilders,
                ImmutableSortedSet.Builder::build);
    }

    private static <T, C extends ImmutableCollection.Builder<T>> C combineBuilders(C left, C right) {
        left.addAll(right.build());
        return left;
    }

    /**
     * @return a collector into Guava's {@code Optional<T>}
     */
    public static <T> Collector<T, ?, Optional<T>> toOptional() {
        return Collector.<T, GuavaOptionalBuilder<T>, Optional<T>>of(
                GuavaOptionalBuilder::new,
                GuavaOptionalBuilder::set,
                (left, right) -> left.build().isPresent() ? left : right,
                GuavaOptionalBuilder::build);
    }

    private static class GuavaOptionalBuilder<T> {

        private Optional<T> optional = Optional.absent();

        public void set(T value) {
            optional = Optional.of(value);
        }

        public Optional<T> build() {
            return optional;
        }
    }

    /**
     * @return a {@code CollectorFactory} which knows how to create all supported Guava types
     */
    public static CollectorFactory factory() {
        return new Factory();
    }

    public static class Factory implements CollectorFactory {

        private static final Map<Class<?>, Collector<?, ?, ?>> collectors =
            ImmutableMap.<Class<?>, Collector<?, ?, ?>>builder()
                .put(ImmutableList.class, toImmutableList())
                .put(ImmutableSet.class, toImmutableSet())
                .put(ImmutableSortedSet.class, toImmutableSortedSet())
                .put(Optional.class, toOptional())
                .build();

        @Override
        public boolean accepts(Type type) {
            return collectors.containsKey(getErasedType(type));
        }

        @SuppressWarnings("unchecked")
        @Override
        public Collector<?, ?, ?> newCollector(Type type) {
            return collectors.get(getErasedType(type));
        }
    }
}

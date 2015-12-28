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
import java.util.Map;
import java.util.stream.Collector;

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
                (builder, element) -> builder.add(element),
                (left, right) -> { left.addAll(right.build()); return left; },
                ImmutableList.Builder::build);
    }

    /**
     * @return a collector into {@code ImmutableSet<T>}
     */
    public static <T> Collector<T, ?, ImmutableSet<T>> toImmutableSet() {
        return Collector.<T, ImmutableSet.Builder<T>, ImmutableSet<T>>of(
                ImmutableSet::builder,
                (builder, element) -> builder.add(element),
                (left, right) -> { left.addAll(right.build()); return left; },
                ImmutableSet.Builder::build);
    }

    /**
     * @return a collector into {@code ImmutableSortedSet<T>}
     */
    public static <T extends Comparable<T>> Collector<T, ?, ImmutableSortedSet<T>> toImmutableSortedSet() {
        return Collector.<T, ImmutableSortedSet.Builder<T>, ImmutableSortedSet<T>>of(
                ImmutableSortedSet::naturalOrder,
                (builder, element) -> builder.add(element),
                (left, right) -> { left.addAll(right.build()); return left; },
                ImmutableSortedSet.Builder::build);
    }


    /**
     * @return a {@code CollectorFactory} which knows how to create all supported Guava types
     */
    public static CollectorFactory factory() {
        return new Factory<>();
    }

    private static class Factory<T> implements CollectorFactory {

        private static final Map<Class<?>, Collector<?, ?, ?>> collectors =
            ImmutableMap.<Class<?>, Collector<?, ?, ?>>builder()
                .put(ImmutableList.class, toImmutableList())
                .put(ImmutableSet.class, toImmutableSet())
                .put(ImmutableSortedSet.class, toImmutableSortedSet())
                .build();

        @Override
        public boolean accepts(Type type) {
            return collectors.containsKey(getErasedType(type));
        }

        @SuppressWarnings("unchecked")
        @Override
        public Collector<T, ?, ? extends Collection<T>> newCollector(Type type) {
            return (Collector<T, ?, ? extends Collection<T>>) collectors.get(getErasedType(type));
        }
    }
}

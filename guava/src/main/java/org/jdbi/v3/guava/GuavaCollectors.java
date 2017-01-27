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

import static org.jdbi.v3.core.generic.GenericTypes.findGenericParameter;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collector;

import org.jdbi.v3.core.collector.CollectorFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

/**
 * Provide Collector instances that create Guava collection types, especially immutable
 * versions of the base Java collections.
 */
public class GuavaCollectors {

    /**
     * @return a {@code CollectorFactory} which knows how to create all supported Guava types
     */
    public static CollectorFactory factory() {
        return new Factory();
    }

    /**
     * @param <T> the element type collected.
     *
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
            if (optional.isPresent()) {
                throw new IllegalStateException(
                        String.format("Multiple values for Optional type: ['%s','%s',...]",
                                optional.get(),
                                value));
            }
            optional = Optional.of(value);
        }

        public Optional<T> build() {
            return optional;
        }
    }

    public static class Factory implements CollectorFactory {

        private static final Map<Class<?>, Collector<?, ?, ?>> collectors =
            ImmutableMap.<Class<?>, Collector<?, ?, ?>>builder()
                .put(ImmutableList.class, ImmutableList.toImmutableList())
                .put(ImmutableSet.class, ImmutableSet.toImmutableSet())
                .put(ImmutableSortedSet.class, ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()))
                .put(Optional.class, toOptional())
                .build();

        @Override
        public boolean accepts(Type containerType) {
            return collectors.containsKey(getErasedType(containerType));
        }

        @Override
        public java.util.Optional<Type> elementType(Type containerType) {
            return findGenericParameter(containerType, getErasedType(containerType));
        }

        @Override
        public Collector<?, ?, ?> build(Type containerType) {
            return collectors.get(getErasedType(containerType));
        }
    }
}

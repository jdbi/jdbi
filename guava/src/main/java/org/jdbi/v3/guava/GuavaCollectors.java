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

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeMultimap;
import org.jdbi.v3.core.collector.CollectorFactory;
import org.jdbi.v3.core.collector.OptionalCollectors;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.jdbi.v3.core.internal.UtilityClassException;

import static org.jdbi.v3.core.collector.MapCollectors.toMap;
import static org.jdbi.v3.core.generic.GenericTypes.findGenericParameter;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;
import static org.jdbi.v3.core.generic.GenericTypes.resolveMapEntryType;
import static org.jdbi.v3.core.generic.GenericTypes.resolveType;

/**
 * Provides Collectors for Guava collection types.
 * <p>Supported container types:</p>
 * <ul>
 * <li>com.google.common.base.Optional&lt;T&gt; (throws an exception if more than one row in result)</li>
 * <li>com.google.common.collect.ImmutableList&lt;T&gt;</li>
 * <li>com.google.common.collect.ImmutableSet&lt;T&gt;</li>
 * <li>com.google.common.collect.ImmutableSortedSet&lt;T extends Comparable&gt;</li>
 * </ul>
 * <p>Supported Maps and Multimaps types - for rows mapped to Map.Entry&lt;K, V&gt;:</p>
 * <ul>
 * <li>com.google.common.collect.BiMap&lt;K, V&gt;</li>
 * <li>com.google.common.collect.ImmutableMap&lt;K, V&gt;</li>
 * <li>com.google.common.collect.Multimap&lt;K, V&gt;</li>
 * <li>com.google.common.collect.ListMultimap&lt;K, V&gt;</li>
 * <li>com.google.common.collect.ArrayListMultimap&lt;K, V&gt;</li>
 * <li>com.google.common.collect.LinkedListMultimap&lt;K, V&gt;</li>
 * <li>com.google.common.collect.SetMultimap&lt;K, V&gt;</li>
 * <li>com.google.common.collect.HashMultimap&lt;K, V&gt;</li>
 * <li>com.google.common.collect.TreeMultimap&lt;K, V&gt;</li>
 * <li>com.google.common.collect.ImmutableMultimap&lt;K, V&gt;</li>
 * <li>com.google.common.collect.ImmutableListMultimap&lt;K, V&gt;</li>
 * <li>com.google.common.collect.ImmutableSetMultimap&lt;K, V&gt;</li>
 * </ul>
 */
public class GuavaCollectors {
    private GuavaCollectors() {
        throw new UtilityClassException();
    }

    /**
     * @return a {@code CollectorFactory} which knows how to create all supported Guava types
     */
    public static CollectorFactory factory() {
        return new Factory();
    }

    /**
     * Returns a {@code Collector} that accumulates 0 or 1 input elements into Guava's {@code Optional<T>}.
     * The returned collector will throw {@code IllegalStateException} whenever 2 or more elements
     * are present in a stream. Null elements are mapped to {@code Optional.absent()}.
     *
     * @param <T> the collected type
     * @return a {@code Collector} which collects 0 or 1 input elements into a Guava {@code Optional<T>}.
     */
    public static <T> Collector<T, ?, Optional<T>> toOptional() {
        return OptionalCollectors.toOptional(Optional::absent, Optional::of);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry<K, V>} input elements into an
     * {@code ImmutableMap<K, V>}.
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @return a {@code Collector} which collects map entry elements into an {@code ImmutableMap},
     * in encounter order.
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, ImmutableMap<K, V>> toImmutableMap() {
        return ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry<K, V>} input elements into a
     * {@code HashBiMap<K, V>}. The returned collector will throw {@code IllegalStateException}
     * whenever a set of input elements contains multiple entries with the same key.
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @return a {@code Collector} which collects map entry elements into a {@code HashBiMap},
     * in encounter order.
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, BiMap<K, V>> toHashBiMap() {
        return toMap(HashBiMap::create);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry<K, V>} input elements into an
     * {@code ImmutableListMultimap<K, V>}.
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @return a {@code Collector} which collects map entry elements into an {@code ImmutableListMultimap},
     * in encounter order.
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, ImmutableListMultimap<K, V>> toImmutableListMultimap() {
        return Collector.of(
                ImmutableListMultimap::<K, V>builder,
                ImmutableListMultimap.Builder::put,
                GuavaCollectors::combine,
                ImmutableListMultimap.Builder::build);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry<K, V>} input elements into an
     * {@code ImmutableSetMultimap<K, V>}.
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @return a {@code Collector} which collects map entry elements into an {@code ImmutableSetMultimap},
     * in encounter order.
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, ImmutableSetMultimap<K, V>> toImmutableSetMultimap() {
        return Collector.of(
                ImmutableSetMultimap::<K, V>builder,
                ImmutableSetMultimap.Builder::put,
                GuavaCollectors::combine,
                ImmutableSetMultimap.Builder::build);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry<K, V>} input elements into an
     * {@code ArrayListMultimap<K, V>}.
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @return a {@code Collector} which collects map entry elements into an {@code ArrayListMultimap},
     * in encounter order.
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, ArrayListMultimap<K, V>> toArrayListMultimap() {
        return toMultimap(ArrayListMultimap::create);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry<K, V>} input elements into a
     * {@code LinkedListMultimap<K, V>}.
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @return a {@code Collector} which collects map entry elements into a {@code LinkedListMultimap},
     * in encounter order.
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, LinkedListMultimap<K, V>> toLinkedListMultimap() {
        return toMultimap(LinkedListMultimap::create);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry<K, V>} input elements into a
     * {@code HashMultimap<K, V>}.
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @return a {@code Collector} which collects map entry elements into a {@code ArrayListMultimap},
     * in encounter order.
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, HashMultimap<K, V>> toHashMultimap() {
        return toMultimap(HashMultimap::create);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry<K, V>} input elements into a
     * {@code TreeMultimap<K, V>}.
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @return a {@code Collector} which collects map entry elements into a {@code TreeMultimap},
     * in encounter order.
     */
    public static <K extends Comparable, V extends Comparable> Collector<Map.Entry<K, V>, ?, TreeMultimap<K, V>> toTreeMultimap() {
        return toMultimap(TreeMultimap::create);
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry<K, V>} input elements into a
     * {@code Multimap<K, V>} of the supplied type.
     *
     * @param <K>             the type of map keys
     * @param <V>             the type of map values
     * @param <M>             a supplier of your multimap type
     * @param multimapFactory a {@code Supplier} which return a new, empty {@code Multimap} of the appropriate type.
     * @return a {@code Collector} which collects map entry elements into a {@code Multiamp}, in encounter order.
     */
    public static <K, V, M extends Multimap<K, V>> Collector<Map.Entry<K, V>, ?, M> toMultimap(Supplier<M> multimapFactory) {
        return Collector.of(
                multimapFactory,
                GuavaCollectors::putEntry,
                GuavaCollectors::combine);
    }

    private static <K, V, M extends Multimap<K, V>> void putEntry(M map, Map.Entry<K, V> entry) {
        map.put(entry.getKey(), entry.getValue());
    }

    private static <K, V, M extends Multimap<K, V>> M combine(M a, M b) {
        a.putAll(b);
        return a;
    }

    private static <K, V, MB extends ImmutableMultimap.Builder<K, V>> MB combine(MB a, MB b) {
        a.putAll(b.build());
        return a;
    }

    private static class Factory implements CollectorFactory {
        private final TypeVariable<Class<Multimap>> multimapKey;
        private final TypeVariable<Class<Multimap>> multimapValue;

        Factory() {
            TypeVariable<Class<Multimap>>[] multimapParams = Multimap.class.getTypeParameters();
            multimapKey = multimapParams[0];
            multimapValue = multimapParams[1];
        }

        private final Map<Class<?>, Collector<?, ?, ?>> collectors =
            ImmutableMap.<Class<?>, Collector<?, ?, ?>>builder()
                .put(ImmutableList.class, ImmutableList.toImmutableList())
                .put(ImmutableSet.class, ImmutableSet.toImmutableSet())
                .put(ImmutableSortedSet.class, ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()))
                .put(Optional.class, toOptional())
                .put(ImmutableMap.class, toImmutableMap())
                .put(BiMap.class, toHashBiMap())
                .put(ImmutableMultimap.class, toImmutableListMultimap())
                .put(ImmutableListMultimap.class, toImmutableListMultimap())
                .put(ImmutableSetMultimap.class, toImmutableSetMultimap())
                .put(Multimap.class, toImmutableListMultimap())
                .put(ListMultimap.class, toImmutableListMultimap())
                .put(ArrayListMultimap.class, toArrayListMultimap())
                .put(LinkedListMultimap.class, toLinkedListMultimap())
                .put(SetMultimap.class, toImmutableSetMultimap())
                .put(HashMultimap.class, toHashMultimap())
                .put(TreeMultimap.class, toTreeMultimap())
                .build();

        @Override
        public boolean accepts(Type containerType) {
            Class<?> erasedType = getErasedType(containerType);
            return collectors.containsKey(erasedType) && containerType instanceof ParameterizedType;
        }

        @Override
        public java.util.Optional<Type> elementType(Type containerType) {
            Class<?> erasedType = getErasedType(containerType);

            if (Multimap.class.isAssignableFrom(erasedType)) {
                Type keyType = resolveType(multimapKey, containerType);
                Type valueType = resolveType(multimapValue, containerType);
                return java.util.Optional.of(resolveMapEntryType(keyType, valueType));
            } else if (Map.class.isAssignableFrom(erasedType)) {
                return java.util.Optional.of(resolveMapEntryType(containerType));
            }

            return findGenericParameter(containerType, erasedType);
        }

        @Override
        public Collector<?, ?, ?> build(Type containerType) {
            return collectors.get(getErasedType(containerType));
        }
    }
}

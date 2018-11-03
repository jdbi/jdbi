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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static org.jdbi.v3.core.generic.GenericTypes.findGenericParameter;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Provides Collectors for built in JDK container types.
 * <p>Supported container types:</p>
 * <ul>
 * <li>java.util.Optional&lt;T&gt; (throws an exception if more than one row in result)</li>
 * <li>java.util.Collection&lt;T&gt;</li>
 * <li>java.util.List&lt;T&gt;</li>
 * <li>java.util.ArrayList&lt;T&gt;</li>
 * <li>java.util.LinkedList&lt;T&gt;</li>
 * <li>java.util.concurrent.CopyOnWriteArrayList&lt;T&gt;</li>
 * <li>java.util.Set&lt;T&gt;</li>
 * <li>java.util.HashSet&lt;T&gt;</li>
 * <li>java.util.LinkedHashSet&lt;T&gt;</li>
 * <li>java.util.SortedSet&lt;T&gt;</li>
 * <li>java.util.TreeSet&lt;T&gt;</li>
 * </ul>
 * <p>Supported Map types - for rows mapped to Map.Entry&lt;K, V&gt;</p>
 * <ul>
 * <li>java.util.Map&lt;K, V&gt;</li>
 * <li>java.util.HashMap&lt;K, V&gt;</li>
 * <li>java.util.LinkedHashMap&lt;K, V&gt;</li>
 * <li>java.util.SortedMap&lt;K, V&gt;</li>
 * <li>java.util.TreeMap&lt;K, V&gt;</li>
 * <li>java.util.concurrent.ConcurrentMap&lt;K, V&gt;</li>
 * <li>java.util.concurrent.ConcurrentHashMap&lt;K, V&gt;</li>
 * <li>java.util.WeakHashMap&lt;K, V&gt;</li>
 * </ul>
 *
 * @deprecated will be replaced by plugin
 */
@Deprecated
public class BuiltInCollectorFactory implements CollectorFactory {
    private static final List<CollectorFactory> FACTORIES = Arrays.asList(
        new MapCollectorFactory(),
        new OptionalCollectorFactory(),
        new ListCollectorFactory(),
        new SetCollectorFactory()
    );

    private final Map<Class<?>, Collector<?, ?, ?>> collectors = new IdentityHashMap<>();

    public BuiltInCollectorFactory() {
    }

    @Override
    public boolean accepts(Type containerType) {
        boolean delegatable = FACTORIES.stream().anyMatch(factory -> factory.accepts(containerType));
        if (delegatable) {
            return true;
        }

        return containerType instanceof ParameterizedType && collectors.containsKey(getErasedType(containerType));
    }

    @Override
    public Optional<Type> elementType(Type containerType) {
        Optional<Type> delegated = FACTORIES.stream()
            .map(factory -> factory.elementType(containerType))
            .filter(Optional::isPresent)
            .findFirst()
            .map(Optional::get);
        if (delegated.isPresent()) {
            return delegated;
        }

        return findGenericParameter(containerType, getErasedType(containerType));
    }

    @Override
    public Collector<?, ?, ?> build(Type containerType) {
        Optional<? extends Collector<?, ?, ?>> delegate = FACTORIES.stream()
            .filter(factory -> factory.accepts(containerType))
            .map(factory -> factory.build(containerType))
            .findFirst();
        if (delegate.isPresent()) {
            return delegate.get();
        }

        Class<?> erasedType = getErasedType(containerType);
        return collectors.get(erasedType);
    }

    /**
     * Returns a {@code Collector} that accumulates 0 or 1 input elements into an {@code Optional<T>}.
     * The returned collector will throw {@code IllegalStateException} whenever 2 or more elements
     * are present in a stream.
     *
     * @param <T> the collected type
     * @return a {@code Collector} which collects 0 or 1 input elements into an {@code Optional<T>}.
     * @deprecated Use {@link OptionalCollectors#toOptional()} instead.
     */
    @Deprecated
    public static <T> Collector<T, ?, Optional<T>> toOptional() {
        return OptionalCollectors.toOptional();
    }

    /**
     * Returns a {@code Collector} that accumulates {@code Map.Entry<K, V>} input elements into a
     * map of the supplied type. The returned collector will throw {@code IllegalStateException}
     * whenever a set of input elements contains multiple entries with the same key.
     *
     * @param <K>        the type of map keys
     * @param <V>        the type of map values
     * @param <M>        the type of the resulting {@code Map}
     * @param mapFactory a {@code Supplier} which returns a new, empty {@code Map} of the appropriate type.
     * @return a {@code Collector} which collects map entry elements into a {@code Map}, in encounter order.
     *
     * @deprecated Use {@link MapCollectors#toMap(Supplier)} instead.
     */
    @Deprecated
    public static <K, V, M extends Map<K, V>> Collector<Map.Entry<K, V>, ?, M> toMap(Supplier<M> mapFactory) {
        return Collector.of(
                mapFactory,
                BuiltInCollectorFactory::putEntry,
                BuiltInCollectorFactory::combine);
    }

    private static <K, V, M extends Map<K, V>> void putEntry(M map, Map.Entry<K, V> entry) {
        putEntry(map, entry.getKey(), entry.getValue());
    }

    private static <K, V, M extends Map<K, V>> void putEntry(M map, K key, V value) {
        V oldValue = map.put(key, value);
        if (oldValue != null) {
            throw new IllegalStateException(String.format(
                    "Multiple values for Map key '%s': ['%s','%s',...]",
                    key,
                    oldValue,
                    value));
        }
    }

    private static <K, V, M extends Map<K, V>> M combine(M a, M b) {
        b.forEach((k, v) -> putEntry(a, k, v));
        return a;
    }
}

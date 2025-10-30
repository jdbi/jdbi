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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;

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
@Deprecated(since = "3.6.0")
public class BuiltInCollectorFactory implements CollectorFactory {
    private static final List<CollectorFactory> FACTORIES = Arrays.asList(
            new MapCollectorFactory(),
            new OptionalCollectorFactory(),
            new ListCollectorFactory(),
            new SetCollectorFactory()
    );

    @Override
    public boolean accepts(Type containerType) {
        return FACTORIES.stream().anyMatch(factory -> factory.accepts(containerType));
    }

    @Override
    public Optional<Type> elementType(Type containerType) {
        return FACTORIES.stream()
                .map(factory -> factory.elementType(containerType))
                .filter(Optional::isPresent)
                .findFirst()
                .map(Optional::get);
    }

    @Override
    public Collector<?, ?, ?> build(Type containerType) {
        return FACTORIES.stream()
                .filter(factory -> factory.accepts(containerType))
                .map(factory -> factory.build(containerType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Unprovidable collector was requested. This is an internal jdbi bug; please report it to the jdbi developers."));
    }
}

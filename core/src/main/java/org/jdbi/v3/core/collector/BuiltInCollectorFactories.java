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

import static org.jdbi.v3.core.generic.GenericTypes.findGenericParameter;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * CollectorFactories for built in JDK and jdbi types.
 * <p>Supported Collection types:</p>
 * <ul>
 *     <li>java.util.Optional&lt;T&gt; (throws an exception if more than one row in result)</li>
 *     <li>java.util.List&lt;T&gt;</li>
 *     <li>java.util.Set&lt;T&gt;</li>
 *     <li>java.util.SortedSet&lt;T&gt;</li>
 *     <li>java.util.Map&lt;K, V&gt; - for row type Map.Entry&lt;K, V&gt;</li>
 * </ul>
 */
public class BuiltInCollectorFactories
{

    /**
     * @return all built in CollectorFactories
     */
    public static Collection<CollectorFactory> get()
    {
        return Arrays.asList(
                fromSupplier(List.class, Collectors::toList),
                fromSupplier(Set.class, Collectors::toSet),
                fromSupplier(SortedSet.class, () -> Collectors.toCollection(TreeSet::new)),
                new MapCollectorFactory(),
                new ArrayCollectorFactory(),
                new OptionalCollectorFactory()
            );
    }

    /**
     * Create a CollectorFactory from collection type and a collector supplier.
     * Most useful for specializing {@link Collection} types since they
     * have a single type parameter.
     * @param <T> the type of the collection
     * @param collectionType the collection type that will eventually be produced
     * @param collector the constructor of collectors
     * @return the CollectorFactory
     */
    public static <T> CollectorFactory fromSupplier(Class<T> collectionType, Supplier<Collector<?, ?, ?>> collector)
    {
        return new CollectorFactory() {
            @Override
            public boolean accepts(Type containerType) {
                return getErasedType(containerType) == collectionType;
            }

            @Override
            public Optional<Type> elementType(Type containerType) {
                return findGenericParameter(containerType, collectionType);
            }

            @Override
            public Collector<?, ?, ?> build(Type containerType) {
                return collector.get();
            }
        };
    }
}

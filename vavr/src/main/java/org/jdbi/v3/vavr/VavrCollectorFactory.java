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
package org.jdbi.v3.vavr;

import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.HashMultimap;
import io.vavr.collection.HashSet;
import io.vavr.collection.IndexedSeq;
import io.vavr.collection.LinearSeq;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.LinkedHashMultimap;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Multimap;
import io.vavr.collection.PriorityQueue;
import io.vavr.collection.Queue;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.collection.SortedMap;
import io.vavr.collection.SortedMultimap;
import io.vavr.collection.SortedSet;
import io.vavr.collection.Stream;
import io.vavr.collection.Traversable;
import io.vavr.collection.TreeMap;
import io.vavr.collection.TreeMultimap;
import io.vavr.collection.TreeSet;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import org.jdbi.v3.core.collector.CollectorFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.stream.Collector;

import static org.jdbi.v3.core.generic.GenericTypes.findGenericParameter;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

class VavrCollectorFactory implements CollectorFactory {

    private final Map<Class<?>, Class<?>> defaultImplementations = HashMap.of(
            Traversable.class, List.class,
            Seq.class, Vector.class,
            IndexedSeq.class, Vector.class,
            LinearSeq.class, List.class,
            Set.class, HashSet.class,
            SortedSet.class, TreeSet.class,
            Map.class, HashMap.class,
            SortedMap.class, TreeMap.class,
            Multimap.class, HashMultimap.class,
            SortedMultimap.class, TreeMultimap.class
    );

    private final Map<Class<?>, Collector<?, ?, ?>> collectors; {
        collectors = HashMap.ofEntries(
                // Values
                Tuple.of(Option.class, VavrCollectors.toOption()),
                // Seqs
                Tuple.of(Array.class, Array.collector()),
                Tuple.of(Vector.class, Vector.collector()),
                Tuple.of(List.class, List.collector()),
                Tuple.of(Stream.class, Stream.collector()),
                Tuple.of(Queue.class, Queue.collector()),
                Tuple.of(PriorityQueue.class, PriorityQueue.collector()),
                // Sets
                Tuple.of(HashSet.class, HashSet.collector()),
                Tuple.of(LinkedHashSet.class, LinkedHashSet.collector()),
                Tuple.of(TreeSet.class, TreeSet.collector()),
                // Maps
                Tuple.of(HashMap.class, HashMap.collector()),
                Tuple.of(LinkedHashMap.class, LinkedHashMap.collector()),
                Tuple.of(TreeMap.class, TreeMap.collector()),
                // Multimaps
                Tuple.of(HashMultimap.class, HashMultimap.withSeq().collector()),
                Tuple.of(LinkedHashMultimap.class, LinkedHashMultimap.withSeq().collector()),
                Tuple.of(TreeMultimap.class, TreeMultimap.withSeq().collector())
        );
    }

    @Override
    public boolean accepts(Type containerType) {
        Class<?> erasedType = getCollectionType(containerType);
        final boolean hasCollector = collectors.containsKey(erasedType);

        return (hasCollector || hasDefaultImplementationWithCollector(erasedType))
                && containerType instanceof ParameterizedType;
    }

    private Class<?> getCollectionType(Type containerType) {
        return getErasedType(containerType);
    }

    private boolean hasDefaultImplementationWithCollector(Class<?> erasedType) {
        return resolveDefaultCollector(erasedType).isDefined();
    }

    private Option<Collector<?, ?, ?>> resolveDefaultCollector(Class<?> erasedType) {
        return defaultImplementations.get(erasedType).flatMap(collectors::get);
    }

    @Override
    public Optional<Type> elementType(Type containerType) {
        Class<?> erasedType = getCollectionType(containerType);

        if (Map.class.isAssignableFrom(erasedType)) {
            return Optional.of(VavrGenericMapUtil.resolveMapEntryType(containerType));
        } else if (Multimap.class.isAssignableFrom(erasedType)) {
            return Optional.of(VavrGenericMapUtil.resolveMultimapEntryType(containerType));
        }

        return findGenericParameter(containerType, erasedType);
    }

    @Override
    public Collector<?, ?, ?> build(Type containerType) {
        Class<?> erasedType = getCollectionType(containerType);
        return collectors.getOrElse(erasedType,
                Lazy.val(() -> resolveDefaultCollector(erasedType).get(), Collector.class));
    }
}

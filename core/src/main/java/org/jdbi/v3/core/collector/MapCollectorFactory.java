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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collector;
import org.jdbi.v3.core.generic.GenericTypes;

import static org.jdbi.v3.core.collector.MapCollectors.toMap;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

class MapCollectorFactory implements CollectorFactory {
    private final Map<Class<?>, Collector<?, ?, ?>> collectors = new HashMap<>();

    MapCollectorFactory() {
        collectors.put(Map.class, toMap(LinkedHashMap::new));
        collectors.put(HashMap.class, toMap(HashMap::new));
        collectors.put(LinkedHashMap.class, toMap(LinkedHashMap::new));
        collectors.put(SortedMap.class, toMap(TreeMap::new));
        collectors.put(TreeMap.class, toMap(TreeMap::new));
        collectors.put(ConcurrentMap.class, toMap(ConcurrentHashMap::new));
        collectors.put(ConcurrentHashMap.class, toMap(ConcurrentHashMap::new));
        collectors.put(WeakHashMap.class, toMap(WeakHashMap::new));
    }

    @Override
    public boolean accepts(Type containerType) {
        Class<?> erasedType = getErasedType(containerType);

        return containerType instanceof ParameterizedType && collectors.containsKey(erasedType);
    }

    @Override
    public Optional<Type> elementType(Type containerType) {
        Class<?> erasedType = getErasedType(containerType);

        return Map.class.isAssignableFrom(erasedType)
            ? Optional.of(GenericTypes.resolveMapEntryType(containerType))
            : Optional.empty();
    }

    @Override
    public Collector<?, ?, ?> build(Type containerType) {
        Class<?> erasedType = getErasedType(containerType);

        return collectors.get(erasedType);
    }
}

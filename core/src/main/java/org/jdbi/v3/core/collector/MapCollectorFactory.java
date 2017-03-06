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

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

class MapCollectorFactory implements CollectorFactory {
    private static final TypeVariable<Class<Map>> KEY_PARAM;
    private static final TypeVariable<Class<Map>> VALUE_PARAM;

    static {
        TypeVariable<Class<Map>>[] mapParams = Map.class.getTypeParameters();
        KEY_PARAM = mapParams[0];
        VALUE_PARAM = mapParams[1];
    }

    private static final Map<Class<?>, Collector<?,?,?>> collectors = ImmutableMap.<Class<?>, Collector<?,?,?>>builder()
            .put(Map.class, toMap(LinkedHashMap::new))
            .put(LinkedHashMap.class, toMap(LinkedHashMap::new))
            .put(HashMap.class, toMap(HashMap::new))
            .put(TreeMap.class, toMap(TreeMap::new))
            .put(SortedMap.class, toMap(TreeMap::new))
            .put(ConcurrentMap.class, toMap(ConcurrentHashMap::new))
            .put(ConcurrentHashMap.class, toMap(ConcurrentHashMap::new))
            .put(WeakHashMap.class, toMap(WeakHashMap::new))
            .build();

    @Override
    public boolean accepts(Type containerType) {
        return collectors.containsKey(getErasedType(containerType))
                && (containerType instanceof ParameterizedType); // no raw Map
    }

    @Override
    public Optional<Type> elementType(Type containerType) {
        TypeToken<?> keyType = TypeToken.of(containerType).resolveType(KEY_PARAM);
        TypeToken<?> valueType = TypeToken.of(containerType).resolveType(VALUE_PARAM);
        return Optional.of(entryType(keyType, valueType));
    }

    private <K, V> Type entryType(TypeToken<K> keyType, TypeToken<V> valueType) {
        return new TypeToken<Map.Entry<K, V>>() {}
                .where(new TypeParameter<K>() {}, keyType)
                .where(new TypeParameter<V>() {}, valueType)
                .getType();
    }

    @Override
    public Collector<?, ?, ?> build(Type containerType) {
        return collectors.get(getErasedType(containerType));
    }

    static <K, V, M extends Map<K, V>> Collector<Map.Entry<K,V>, ?, M> toMap(Supplier<M> supplier) {
        return Collector.of(
                supplier,
                MapCollectorFactory::putEntry,
                MapCollectorFactory::combine);
    }

    private static <K, V, M extends Map<K, V>> void putEntry(M map, Map.Entry<K, V> entry) {
        // TODO throw exception if duplicate entry?
        map.put(entry.getKey(), entry.getValue());
    }

    private static <K, V, M extends Map<K, V>> M combine(M a, M b) {
        // TODO throw exception on duplicate entries?
        a.putAll(b);
        return a;
    }
}

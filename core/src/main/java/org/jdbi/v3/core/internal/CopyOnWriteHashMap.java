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
package org.jdbi.v3.core.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class CopyOnWriteHashMap<K, V> implements ConcurrentMap<K, V> {
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<CopyOnWriteHashMap, Map> U
            = AtomicReferenceFieldUpdater.newUpdater(CopyOnWriteHashMap.class, Map.class, "m");
    @SuppressWarnings("unused")
    private volatile Map<K, V> m;

    public CopyOnWriteHashMap() {
        clear();
    }

    public CopyOnWriteHashMap(Map<K, V> that) {
        if (that.getClass() == CopyOnWriteHashMap.class) {
            set(((CopyOnWriteHashMap<K, V>) that).get());
        } else {
            set(new HashMap<>(that));
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private AtomicReferenceFieldUpdater<CopyOnWriteHashMap<K, V>, Map<K, V>> u() {
        return (AtomicReferenceFieldUpdater) U;
    }

    private Map<K, V> get() {
        return u().get(this);
    }

    private void set(Map<K, V> m) {
        u().set(this, m);
    }

    private Map<K, V> getAndUpdate(UnaryOperator<Map<K, V>> updater) {
        return u().getAndUpdate(this, updater);
    }

    private Map<K, V> updateAndGet(UnaryOperator<Map<K, V>> updater) {
        return u().updateAndGet(this, updater);
    }

    @Override
    public int size() {
        return get().size();
    }

    @Override
    public boolean isEmpty() {
        return get().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return get().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return get().containsValue(value);
    }

    @Override
    public V get(Object key) {
        return get().get(key);
    }

    @Override
    public V put(K key, V value) {
        return getAndUpdate(old -> {
            Map<K, V> updated = new HashMap<>(old);
            updated.put(key, value);
            return updated;
        }).get(key);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return getAndUpdate(old -> {
            if (old.get(key) != null) {
                return old;
            }
            Map<K, V> updated = new HashMap<>(old);
            updated.putIfAbsent(key, value);
            return updated;
        }).get(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> that) {
        updateAndGet(old -> {
            Map<K, V> updated = new HashMap<>(old);
            updated.putAll(that);
            return updated;
        });
    }

    @Override
    public V remove(Object key) {
        return getAndUpdate(old -> {
            if (!old.containsKey(key)) {
                return old;
            }
            Map<K, V> updated = new HashMap<>(old);
            updated.remove(key);
            return updated;
        }).get(key);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return Objects.equals(getAndUpdate(old -> {
            if (!Objects.equals(old.get(key), value)) {
                return old;
            }
            Map<K, V> updated = new HashMap<>(old);
            updated.remove(key, value);
            return updated;
        }).get(key), value);
    }

    @Override
    public V replace(K key, V value) {
        return getAndUpdate(old -> {
            if (!old.containsKey(key)) {
                return old;
            }
            Map<K, V> updated = new HashMap<>(old);
            updated.replace(key, value);
            return updated;
        }).get(key);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return Objects.equals(getAndUpdate(old -> {
            if (!Objects.equals(old.get(key), oldValue)) {
                return old;
            }
            Map<K, V> updated = new HashMap<>(old);
            updated.put(key, newValue);
            return updated;
        }).get(key), oldValue);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return updateAndGet(old -> {
            Map<K, V> updated = new HashMap<>(old);
            updated.compute(key, remappingFunction);
            return updated;
        }).get(key);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return updateAndGet(old -> {
            if (old.containsKey(key)) {
                return old;
            }
            Map<K, V> updated = new HashMap<>(old);
            updated.computeIfAbsent(key, mappingFunction);
            return updated;
        }).get(key);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return updateAndGet(old -> {
            if (!old.containsKey(key)) {
                return old;
            }
            Map<K, V> updated = new HashMap<>(old);
            updated.computeIfPresent(key, remappingFunction);
            return updated;
        }).get(key);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return updateAndGet(old -> {
            Map<K, V> updated = new HashMap<>(old);
            updated.merge(key, value, remappingFunction);
            return updated;
        }).get(key);
    }

    @Override
    public void clear() {
        set(new HashMap<>());
    }

    @Override
    public boolean equals(Object obj) {
        return get().equals(obj);
    }

    @Override
    public int hashCode() {
        return get().hashCode();
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return get().getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        get().forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        updateAndGet(old -> {
            Map<K, V> updated = new HashMap<>(old);
            updated.replaceAll(function);
            return updated;
        });
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(get().keySet());
    }

    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(get().values());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableMap(get()).entrySet();
    }
}

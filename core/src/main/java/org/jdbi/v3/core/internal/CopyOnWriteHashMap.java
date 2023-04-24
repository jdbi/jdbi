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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public final class CopyOnWriteHashMap<K, V> implements ConcurrentMap<K, V> {
    private final AtomicReference<Map<K, V>> m;

    public CopyOnWriteHashMap() {
        m = new AtomicReference<>(new HashMap<>());
    }

    public CopyOnWriteHashMap(Map<K, V> that) {
        if (that.getClass() == CopyOnWriteHashMap.class) {
            m = new AtomicReference<>(((CopyOnWriteHashMap<K, V>) that).m.get());
        } else {
            m = new AtomicReference<>(new HashMap<>(that));
        }
    }

    @Override
    public int size() {
        return m.get().size();
    }

    @Override
    public boolean isEmpty() {
        return m.get().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return m.get().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return m.get().containsValue(value);
    }

    @Override
    public V get(Object key) {
        return m.get().get(key);
    }

    @Override
    public V put(K key, V value) {
        return m.getAndUpdate(old -> {
            Map<K, V> updated = new HashMap<>(old);
            updated.put(key, value);
            return updated;
        }).get(key);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return m.getAndUpdate(old -> {
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
        m.updateAndGet(old -> {
            Map<K, V> updated = new HashMap<>(old);
            updated.putAll(that);
            return updated;
        });
    }

    @Override
    public V remove(Object key) {
        return m.getAndUpdate(old -> {
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
        return Objects.equals(m.getAndUpdate(old -> {
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
        return m.getAndUpdate(old -> {
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
        return Objects.equals(m.getAndUpdate(old -> {
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
        return m.updateAndGet(old -> {
            Map<K, V> updated = new HashMap<>(old);
            updated.compute(key, remappingFunction);
            return updated;
        }).get(key);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return m.updateAndGet(old -> {
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
        return m.updateAndGet(old -> {
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
        return m.updateAndGet(old -> {
            Map<K, V> updated = new HashMap<>(old);
            updated.merge(key, value, remappingFunction);
            return updated;
        }).get(key);
    }

    @Override
    public void clear() {
        m.set(new HashMap<>());
    }

    @Override
    public boolean equals(Object obj) {
        return m.get().equals(obj);
    }

    @Override
    public int hashCode() {
        return m.get().hashCode();
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return m.get().getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        m.get().forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        m.updateAndGet(old -> {
            Map<K, V> updated = new HashMap<>(old);
            updated.replaceAll(function);
            return updated;
        });
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(m.get().keySet());
    }

    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(m.get().values());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new SetEntryEntrySet<>(m.get().entrySet());
    }

    static class SetEntryEntrySet<K, V> extends AbstractSet<Entry<K, V>> {
        private final Set<Entry<K, V>> s;

        private SetEntryEntrySet(Set<Entry<K, V>> s) {
            this.s = s;
        }

        @Override
        public int size() {
            return s.size();
        }

        @Override
        public boolean isEmpty() {
            return s.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return s.contains(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return s.containsAll(c);
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new SetEntryIterator<>(s.iterator());
        }

        @Override
        public Spliterator<Entry<K, V>> spliterator() {
            return new SetEntrySpliterator<>(s.spliterator());
        }

        @Override
        public boolean add(Entry<K, V> e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends Entry<K, V>> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    static class SetEntryIterator<K, V> implements Iterator<Entry<K, V>> {
        private final Iterator<Entry<K, V>> i;

        SetEntryIterator(Iterator<Entry<K, V>> i) {
            this.i = i;
        }

        @Override
        public boolean hasNext() {
            return i.hasNext();
        }

        @Override
        public Entry<K, V> next() {
            return new AbstractMap.SimpleImmutableEntry<>(i.next());
        }
    }

    static class SetEntrySpliterator<K, V> implements Spliterator<Entry<K, V>> {
        private final Spliterator<Entry<K, V>> s;

        SetEntrySpliterator(Spliterator<Entry<K, V>> s) {
            this.s = s;
        }

        @Override
        public boolean tryAdvance(Consumer<? super Entry<K, V>> action) {
            return s.tryAdvance(e -> action.accept(new AbstractMap.SimpleImmutableEntry<>(e)));
        }

        @Override
        public Spliterator<Entry<K, V>> trySplit() {
            Spliterator<Entry<K, V>> split = s.trySplit();
            if (split == null) {
                return null;
            }
            return new SetEntrySpliterator<>(split);
        }

        @Override
        public long estimateSize() {
            return s.estimateSize();
        }

        @Override
        public int characteristics() {
            return s.characteristics() | Spliterator.IMMUTABLE;
        }
    }
}
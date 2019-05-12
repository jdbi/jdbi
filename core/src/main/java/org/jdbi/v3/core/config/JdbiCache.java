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
package org.jdbi.v3.core.config;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.meta.Beta;

/**
 * Simple cache interface.
 *
 * @see JdbiCaches
 */
@Beta
public final class JdbiCache<K, V> {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    private final Function<K, ? extends Object> keyNormalizer;
    private final Function<K, V> computer;
    private int maximumSize = Integer.MAX_VALUE;

    JdbiCache(Function<K, ? extends Object> keyNormalizer,
              Function<K, V> computer) {
        this.keyNormalizer = keyNormalizer;
        this.computer = computer;
    }

    public V get(K key, ConfigRegistry config) {
        JdbiCaches caches = config.get(JdbiCaches.class);
        Map<K, V> map = caches.getMap(this);

        @SuppressWarnings("unchecked")
        K k = (K) keyNormalizer.apply(key);

        if (maximumSize < 1) {
            return computer.apply(k);
        }

        lock.readLock().lock();
        try {
            if (map.containsKey(k)) {
                hits.incrementAndGet();
                return map.get(k);
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            if (map.containsKey(k)) {
                return map.get(k);
            }

            misses.incrementAndGet();

            V value = computer.apply(k);

            if (map.size() + 1 > maximumSize) {
                Iterator<K> iterator = map.keySet().iterator();
                while (map.size() + 1 > maximumSize) {
                    iterator.next();
                    iterator.remove();
                }
            }

            map.put(k, value);

            return value;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public V get(K key, Configurable<?> configurable) {
        return get(key, configurable.getConfig());
    }

    public V get(K key, StatementContext ctx) {
        return get(key, ctx.getConfig());
    }

    public JdbiCache<K, V> maximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
        return this;
    }

    String stats() {
        long hits = this.hits.longValue();
        long misses = this.misses.longValue();
        long total = hits + misses;
        double percent = total > 0 ? 100.0 * hits / total : 100.0;
        return String.format("%s hits, %s misses: %s percent hit rate", hits, misses, percent);
    }
}

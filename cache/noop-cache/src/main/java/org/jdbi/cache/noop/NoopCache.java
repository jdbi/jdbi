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
package org.jdbi.cache.noop;

import org.jdbi.core.cache.JdbiCache;
import org.jdbi.core.cache.JdbiCacheBuilder;
import org.jdbi.core.cache.JdbiCacheLoader;

/**
 * A no operation cache implementation.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public final class NoopCache<K, V> implements JdbiCache<K, V> {

    static final Object NOOP_CACHE_STATS = new Object();

    private final JdbiCacheLoader<K, V> cacheLoader;

    /**
     * Returns a new {@link JdbiCacheBuilder} which can be used to construct the internal caches.
     *
     * @return A {@link JdbiCacheBuilder} instance.
     */
    public static JdbiCacheBuilder builder() {
        return new NoopCacheBuilder();
    }

    NoopCache(JdbiCacheLoader<K, V> cacheLoader) {
        this.cacheLoader = cacheLoader;
    }

    @Override
    public V get(K key) {
        if (cacheLoader != null) {
            return cacheLoader.create(key);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public V getWithLoader(K key, JdbiCacheLoader<K, V> loader) {
        return loader.create(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getStats() {
        return NOOP_CACHE_STATS;
    }
}

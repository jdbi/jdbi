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
package org.jdbi.cache.caffeine;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.jdbi.core.cache.JdbiCache;
import org.jdbi.core.cache.JdbiCacheLoader;

/**
 * Cache implementation using the caffeine cache library.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 * @deprecated should not be public API
 */
@Deprecated(forRemoval = true, since = "3.50")
public final class CaffeineLoadingCache<K, V> implements JdbiCache<K, V> {

    private final LoadingCache<K, V> loadingCache;

    CaffeineLoadingCache(Caffeine<Object, Object> caffeine, JdbiCacheLoader<K, V> cacheLoader) {

        this.loadingCache = caffeine.build(cacheLoader::create);
    }

    @Override
    public V get(K key) {
        return loadingCache.get(key);
    }

    @Override
    public V getWithLoader(K key, JdbiCacheLoader<K, V> loader) {
        return loadingCache.get(key, loader::create);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CacheStats getStats() {
        return loadingCache.stats();
    }
}

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
package org.jdbi.v3.cache.caffeine;

import java.util.concurrent.CompletableFuture;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jdbi.v3.core.cache.JdbiCache;
import org.jdbi.v3.core.cache.JdbiCacheLoader;

class JdbiCaffeineCache<K, V> implements JdbiCache<K, V> {
    private final Cache<K, CompletableFuture<V>> cache;
    private final JdbiCacheLoader<K, V> loader;

    JdbiCaffeineCache(Caffeine<Object, Object> caffeine, JdbiCacheLoader<K, V> loader) {
        this.loader = loader;
        this.cache = caffeine.build();
    }

    @Override
    public V get(K key) {
        return doGet(key, loader);
    }

    @Override
    public V getWithLoader(K key, JdbiCacheLoader<K, V> myLoader) {
        return doGet(key, myLoader);
    }

    // Future used as fine-grained lock instead of cache stripe lock
    // https://github.com/jdbi/jdbi/issues/2834
    @SuppressFBWarnings("JLM_JSR166_UTILCONCURRENT_MONITORENTER")
    private V doGet(K key, JdbiCacheLoader<K, V> loader) {
        CompletableFuture<V> future = cache.asMap().computeIfAbsent(key, k -> new CompletableFuture<>());
        if (future.isDone()) {
            return future.join();
        }
        synchronized (future) {
            if (!future.isDone()) {
                future.complete(loader == null ? null : loader.create(key));
            }
            return future.join();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public CacheStats getStats() {
        return cache.stats();
    }
}

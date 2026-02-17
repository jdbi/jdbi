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
package org.jdbi.core.cache;

/**
 * A generic cache implementation for JDBI internal use.
 * <br/>
 * All implementations of this interface must be threadsafe!
 *
 * @param <K> A key type for the cache.
 * @param <V> A value type for the cache.
 */
public interface JdbiCache<K, V> {

    /**
     * Returns the cached value for a key.
     *
     * @param key The key value. Must not be null.
     * @return The cached value or null if no value was cached.
     */
    V get(K key);

    /**
     * Returns a cached value for a key. If no value is cached, create a new value using the
     * provided cache loader.
     *
     * @param key The key value. Must not be null.
     * @param loader A {@link JdbiCacheLoader} implementation. May be called with the provided key value.
     * @return The cached value or null if no value was cached.
     */
    V getWithLoader(K key, JdbiCacheLoader<K, V> loader);

    /**
     * Return implementation specific statistics for the cache object. This can be used to expose
     * statistic information about the underlying implementation.
     *
     * @param <T> The type of the statistics object
     * @return An implementation specific object
     */
    <T> T getStats();
}

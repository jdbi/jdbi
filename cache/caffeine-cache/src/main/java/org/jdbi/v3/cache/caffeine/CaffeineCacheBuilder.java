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

import com.github.benmanes.caffeine.cache.Caffeine;
import org.jdbi.v3.core.cache.JdbiCache;
import org.jdbi.v3.core.cache.JdbiCacheBuilder;
import org.jdbi.v3.core.cache.JdbiCacheLoader;

/**
 * Cache builder using the Caffeine caching library.
 */
public final class CaffeineCacheBuilder implements JdbiCacheBuilder {

    private final Caffeine<Object, Object> caffeine;

    /**
     * Returns a new {@link JdbiCacheBuilder} which can be used to construct the internal caches.
     *
     * @return A {@link JdbiCacheBuilder} instance.
     */
    public static JdbiCacheBuilder instance() {
        return new CaffeineCacheBuilder();
    }

    /**
     * Wraps an existing {@link Caffeine} object for Jdbi internal use.
     *
     * @param caffeine A {@link Caffeine} object.
     */
    public CaffeineCacheBuilder(Caffeine<Object, Object> caffeine) {
        this.caffeine = caffeine;
    }

    CaffeineCacheBuilder() {
        this.caffeine = Caffeine.newBuilder().recordStats();
    }

    @Override
    public <K, V> JdbiCache<K, V> build() {
        return new JdbiCaffeineCache<>(caffeine, null);
    }

    @Override
    public <K, V> JdbiCache<K, V> buildWithLoader(JdbiCacheLoader<K, V> cacheLoader) {
        return new JdbiCaffeineCache<>(caffeine, cacheLoader);
    }

    @Override
    public JdbiCacheBuilder maxSize(int maxSize) {
        caffeine.maximumSize(maxSize);
        return this;
    }
}

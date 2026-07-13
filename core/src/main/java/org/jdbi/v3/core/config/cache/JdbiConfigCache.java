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
package org.jdbi.v3.core.config.cache;

import java.util.concurrent.ConcurrentHashMap;

import org.inferred.freebuilder.shaded.com.google.common.cache.CacheStats;

/**
 * Caching class for any config related caches. Keeps track of various statistics that are useful for
 * performance measurement.
 *
 * @param <K> key type of the cache
 * @param <V> value type of the cache
 */
public final class JdbiConfigCache<K, V> {

    private final String configName;
    private final ConcurrentHashMap<K, V> cache;

    private final JdbiConfigCacheMetrics cacheMetrics = new JdbiConfigCacheMetrics();
    private final JdbiConfigCacheMetrics globalMetrics;

    public JdbiConfigCache(String configName) {
        this.configName = configName;
        this.cache = new ConcurrentHashMap<>();
        this.globalMetrics = new JdbiConfigCacheMetrics();
    }

    private JdbiConfigCache(JdbiConfigCache<K, V> that) {
        this.configName = that.configName;
        this.cache = new ConcurrentHashMap<>(that.cache);
        this.globalMetrics = that.globalMetrics;
    }

    public JdbiConfigCache<K, V> copy() {
        return new JdbiConfigCache<>(this);
    }

    public void clear() {
        this.cache.clear();
    }

    public V put(K key, V value) {
        globalMetrics.put();
        cacheMetrics.put();
        return this.cache.put(key, value);
    }

    public V putIfAbsent(K key, V value) {
        globalMetrics.put();
        cacheMetrics.put();
        return this.cache.putIfAbsent(key, value);
    }

    public V get(K key) {
        V value = cache.get(key);
        if (value != null) {
            globalMetrics.hit();
            cacheMetrics.hit();
        } else {
            globalMetrics.miss();
            cacheMetrics.miss();
        }
        return value;
    }

    public JdbiConfigCacheStats getStats() {
        return new JdbiConfigCacheStats(configName, cache.size(), cacheMetrics, globalMetrics);
    }

}

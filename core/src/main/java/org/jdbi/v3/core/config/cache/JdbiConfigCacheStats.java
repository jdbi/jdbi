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

import java.util.StringJoiner;

@SuppressWarnings("PMD.DataClass")
public final class JdbiConfigCacheStats {

    private final String configName;
    private final int size;
    private final int cacheHits;
    private final int cacheMisses;
    private final int cacheWrites;
    private final int globalHits;
    private final int globalMisses;
    private final int globalWrites;

    JdbiConfigCacheStats(String configName, int cacheSize, JdbiConfigCacheMetrics cacheMetrics, JdbiConfigCacheMetrics globalMetrics) {
        this.configName = configName;
        this.size = cacheSize;
        this.cacheHits = cacheMetrics.getCacheHits();
        this.cacheMisses = cacheMetrics.getCacheMisses();
        this.cacheWrites = cacheMetrics.getCacheWrites();
        this.globalHits = globalMetrics.getCacheHits();
        this.globalMisses = globalMetrics.getCacheMisses();
        this.globalWrites = globalMetrics.getCacheWrites();
    }

    public String configName() {
        return configName;
    }

    public String getConfigName() {
        return configName;
    }

    public int getSize() {
        return size;
    }

    public int getCacheHits() {
        return cacheHits;
    }

    public int getCacheMisses() {
        return cacheMisses;
    }

    public int getCacheAccesses() {
        return cacheMisses + cacheHits;
    }

    public int getCacheWrites() {
        return cacheWrites;
    }

    public int getGlobalHits() {
        return globalHits;
    }

    public int getGlobalMisses() {
        return globalMisses;
    }

    public int getGlobalAccesses() {
        return globalMisses + globalHits;
    }

    public int getGlobalWrites() {
        return globalWrites;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", JdbiConfigCacheStats.class.getSimpleName() + "[", "]")
            .add("configName='" + configName + "'")
            .add("size=" + size)
            .add("cacheHits=" + cacheHits)
            .add("cacheMisses=" + cacheMisses)
            .add("cacheWrites=" + cacheWrites)
            .add("globalHits=" + globalHits)
            .add("globalMisses=" + globalMisses)
            .add("globalWrites=" + globalWrites)
            .toString();
    }
}

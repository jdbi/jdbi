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
package org.jdbi.core.cache.internal;

/**
 * Simple statistics for an {@link DefaultJdbiCache} instance. The values in this object are a snapshot of
 * the cache status. Calling any method multiple times is cheap and constant time.
 */
public final class DefaultJdbiCacheStats {

    private final int cacheSize;
    private final int maxSize;

    DefaultJdbiCacheStats(int cacheSize, int maxSize) {
        this.cacheSize = cacheSize;
        this.maxSize = maxSize;
    }

    /**
     * Returns the current size of the cache.
     *
     * @return The current size of the cache.
     */
    public int cacheSize() {
        return cacheSize;
    }

    /**
     * Returns the maximum size of the cache.
     *
     * @return The maximum size of the cache.
     */
    public int maxSize() {
        return maxSize;
    }
}

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
package org.jdbi.v3.core.cache.internal;

import org.jdbi.v3.core.cache.JdbiCache;
import org.jdbi.v3.core.cache.JdbiCacheBuilder;
import org.jdbi.v3.core.cache.JdbiCacheLoader;

/**
 * Builder for the default Jdbi cache implementation.
 */
public final class DefaultJdbiCacheBuilder implements JdbiCacheBuilder {

    private int maxSize = -1;

    /**
     * Returns a new Builder.
     * @return A new builder instance for a {@link DefaultJdbiCache} instance.
     */
    public static DefaultJdbiCacheBuilder builder() {
        return new DefaultJdbiCacheBuilder();
    }

    private DefaultJdbiCacheBuilder() {}

    @Override
    public <K, V> JdbiCache<K, V> build() {
        return new DefaultJdbiCache<>(this, null);
    }

    @Override
    public <K, V> JdbiCache<K, V> buildWithLoader(JdbiCacheLoader<K, V> loader) {
        return new DefaultJdbiCache<>(this, loader);
    }

    @Override
    public DefaultJdbiCacheBuilder maxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    int getMaxSize() {
        return maxSize;
    }
}

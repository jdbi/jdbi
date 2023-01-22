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
package org.jdbi.v3.cache.noop;

import org.jdbi.v3.core.cache.JdbiCache;
import org.jdbi.v3.core.cache.JdbiCacheBuilder;
import org.jdbi.v3.core.cache.JdbiCacheLoader;

/**
 * Cache builder for the no op cache.
 */
public final class NoopCacheBuilder implements JdbiCacheBuilder {

    NoopCacheBuilder() {}

    @Override
    public <K, V> JdbiCache<K, V> build() {
        return new NoopCache<>(null);
    }

    @Override
    public <K, V> JdbiCache<K, V> buildWithLoader(JdbiCacheLoader<K, V> cacheLoader) {
        return new NoopCache<>(cacheLoader);
    }

    @Override
    public JdbiCacheBuilder maxSize(int maxSize) {
        return this;
    }
}

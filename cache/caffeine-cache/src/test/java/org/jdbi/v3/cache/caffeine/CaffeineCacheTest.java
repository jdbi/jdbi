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

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.jdbi.v3.core.cache.internal.JdbiCacheTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaffeineCacheTest extends JdbiCacheTest {

    @BeforeEach
    void beforeEach() {
        this.builder = CaffeineCacheBuilder.instance();
    }

    @Test
    void testCaffeineWithGlobalLoader() {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder().expireAfterAccess(Duration.ofSeconds(5)).initialCapacity(10);
        doTestWithGlobalLoader(new CaffeineCacheBuilder(caffeine).buildWithLoader(cacheLoader));
    }

    @Test
    void testCaffeineWithDirectLoader() {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder().expireAfterAccess(Duration.ofSeconds(5)).initialCapacity(10);
        doTestWithLoader(new CaffeineCacheBuilder(caffeine).build());
    }
}

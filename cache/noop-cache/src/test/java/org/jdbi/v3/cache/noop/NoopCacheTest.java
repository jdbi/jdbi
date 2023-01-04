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

import java.util.UUID;

import org.jdbi.v3.core.cache.JdbiCache;
import org.jdbi.v3.core.cache.internal.JdbiCacheTest;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;

class NoopCacheTest extends JdbiCacheTest {

    @BeforeEach
    void beforeEach() {
        this.builder = NoopCache.builder();
    }

    protected void doTestWithGlobalLoader(JdbiCache<String, String> cache) {
        assertThat(cacheLoader.created()).isZero();

        String key = UUID.randomUUID().toString();

        // cache creation. creation event.
        String value = cache.get(key);
        assertThat(value).isEqualTo(cacheLoader.checkKey(key));
        assertThat(cacheLoader.created()).isOne();

        // no cache. another creation event
        value = cache.get(key);
        assertThat(value).isEqualTo(cacheLoader.checkKey(key));
        assertThat(cacheLoader.created()).isEqualTo(2);

        String key2 = UUID.randomUUID().toString();

        // cache creation. next creation event.
        String value2 = cache.get(key2);
        assertThat(cacheLoader.created()).isEqualTo(3);
        assertThat(value2).isEqualTo(cacheLoader.checkKey(key2));
    }

    protected void doTestWithLoader(JdbiCache<String, String> cache) {
        assertThat(cacheLoader.created()).isZero();

        String key = UUID.randomUUID().toString();

        // cache creation. creation event.
        String value = cache.getWithLoader(key, cacheLoader);
        assertThat(value).isEqualTo(cacheLoader.checkKey(key));
        assertThat(cacheLoader.created()).isOne();

        // no cache. another creation event
        value = cache.getWithLoader(key, cacheLoader);
        assertThat(value).isEqualTo(cacheLoader.checkKey(key));
        assertThat(cacheLoader.created()).isEqualTo(2);

        String key2 = UUID.randomUUID().toString();

        // cache creation. second creation event.
        String value2 = cache.getWithLoader(key2, cacheLoader);
        assertThat(cacheLoader.created()).isEqualTo(3);
        assertThat(value2).isEqualTo(cacheLoader.checkKey(key2));
    }
}

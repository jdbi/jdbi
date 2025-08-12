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

import java.util.UUID;

import org.jdbi.v3.core.cache.JdbiCache;
import org.jdbi.v3.core.cache.JdbiCacheBuilder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultJdbiCacheTest extends JdbiCacheTest {

    @Override
    protected JdbiCacheBuilder setupBuilder() {
        return DefaultJdbiCacheBuilder.builder();
    }

    @Test
    void testUntouchedCacheExpunge() {
        int keyCount = 0;

        int size = 10;
        JdbiCache<String, String> cache = setupBuilder().maxSize(size).buildWithLoader(cacheLoader);

        // tests specific DefaultJdbiCache LRU behavior. Skip for all other cache types.
        Assumptions.assumeTrue(cache instanceof DefaultJdbiCache);

        DefaultJdbiCacheStats stats = cache.getStats();
        assertThat(stats.maxSize()).isEqualTo(size);

        String[] keys = new String[size * 2];

        for (int i = 0; i < keys.length; i++) {
            keys[i] = keyCount++ + "K_" + UUID.randomUUID();
            cache.get(keys[i]);
        }

        assertThat(cacheLoader.created()).isEqualTo(size * 2);

        stats = cache.getStats();
        assertThat(stats.cacheSize()).isEqualTo(stats.maxSize());

        // test LRU behavior. Change this part if the algorithm changes

        // last elements are still in the cache, b/c were not touched.
        for (int i = keys.length - 1; i >= size; i--) {
            assertThat(cacheLoader.created()).isEqualTo(size * 2);
            cache.get(keys[i]);
            // no additional cache hit
            assertThat(cacheLoader.created()).isEqualTo(size * 2);
        }

        // first were expunged
        for (int i = 0; i < size; i++) {
            int creations = cacheLoader.created();
            cache.get(keys[i]);
            // additional creation hit
            assertThat(creations + 1).isEqualTo(cacheLoader.created());
        }
    }

    @Test
    void testLruCacheExpunge() {
        int keyCount = 0;

        int size = 10;
        JdbiCache<String, String> cache = setupBuilder().maxSize(size).buildWithLoader(cacheLoader);

        // tests specific DefaultJdbiCache LRU behavior. Skip for all other cache types.
        Assumptions.assumeTrue(cache instanceof DefaultJdbiCache);

        DefaultJdbiCacheStats stats = cache.getStats();
        assertThat(stats.maxSize()).isEqualTo(size);

        String[] keys = new String[size * 2];

        // add 20 keys to a 10 size cache
        for (int i = 0; i < keys.length; i++) {
            keys[i] = keyCount++ + "K_" + UUID.randomUUID();
            cache.get(keys[i]);

            // refresh keys 0 - 4 constantly so they don't drop.
            int cacheSize = Math.min(i + 1, size / 2);
            for (int j = 0; j < cacheSize; j++) {
                // does not cause an additional creation event (cache hit)
                int creations = cacheLoader.created();
                cache.get(keys[j]);
                assertThat(cacheLoader.created()).isEqualTo(creations);
            }
        }

        assertThat(cacheLoader.created()).isEqualTo(size * 2);

        // cache now holds 0..4 and 15..19

        stats = cache.getStats();
        assertThat(stats.cacheSize()).isEqualTo(stats.maxSize());

        // test LRU behavior. Change this part if the algorithm changes

        for (int i = 0; i < size / 2; i++) {
            // test that 0..4 come from cache, don't cause creation events
            assertThat(cacheLoader.created()).isEqualTo(size * 2);
            cache.get(keys[i]);
            // no additional cache hit
            assertThat(cacheLoader.created()).isEqualTo(size * 2);

            // test that 19..15 come from cache, don't cause creation events
            int upperKey = keys.length - 1 - i;
            cache.get(keys[upperKey]);
            // no additional cache hit
            assertThat(cacheLoader.created()).isEqualTo(size * 2);

        }

        // 5..14 cause creation events and evict 0..4 and 15..19
        for (int i = size / 2; i < (size * 3) / 2; i++) {
            int creations = cacheLoader.created();
            cache.get(keys[i]);
            // creation hit
            assertThat(creations + 1).isEqualTo(cacheLoader.created());
        }

        // cache now holds 5..14

        // testing for 0..5 and 15..19 now causes all creation events
        for (int i = 0; i < size / 2; i++) {
            int creations = cacheLoader.created();
            cache.get(keys[i]);
            // creation hit
            assertThat(creations + 1).isEqualTo(cacheLoader.created());

            int upperKey = keys.length - 1 - i;
            cache.get(keys[upperKey]);
            // creation hit
            assertThat(creations + 2).isEqualTo(cacheLoader.created());
        }

    }

    static void refresh(int max, JdbiCache<String, String> cache, String[] keys) {
        for (int i = 0; i < max; i++) {
            cache.get(keys[i]);
        }
    }

}

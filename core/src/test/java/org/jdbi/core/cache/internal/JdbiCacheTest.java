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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jdbi.core.cache.JdbiCache;
import org.jdbi.core.cache.JdbiCacheBuilder;
import org.jdbi.core.cache.JdbiCacheLoader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class JdbiCacheTest {

    protected TestingCacheLoader cacheLoader = new TestingCacheLoader();

    protected abstract JdbiCacheBuilder setupBuilder();

    @Test
    void testWithGlobalLoader() {
        doTestWithGlobalLoader(setupBuilder().buildWithLoader(cacheLoader));
    }

    @Test
    void testWithDirectLoader() {
        doTestWithLoader(setupBuilder().build());
    }

    protected void doTestWithGlobalLoader(JdbiCache<String, String> cache) {
        assertThat(cacheLoader.created()).isZero();

        String key = UUID.randomUUID().toString();

        // cache creation. creation event.
        String value = cache.get(key);
        assertThat(value).isEqualTo(cacheLoader.checkKey(key));
        assertThat(cacheLoader.created()).isOne();

        // cache hit. Same value, no creation event.
        value = cache.get(key);
        assertThat(value).isEqualTo(cacheLoader.checkKey(key));
        assertThat(cacheLoader.created()).isOne();

        String key2 = UUID.randomUUID().toString();

        // cache creation. second creation event.
        String value2 = cache.get(key2);
        assertThat(cacheLoader.created()).isEqualTo(2);
        assertThat(value2).isEqualTo(cacheLoader.checkKey(key2));
    }

    protected void doTestWithLoader(JdbiCache<String, String> cache) {
        assertThat(cacheLoader.created()).isZero();

        String key = UUID.randomUUID().toString();

        // cache creation. creation event.
        String value = cache.getWithLoader(key, cacheLoader);
        assertThat(value).isEqualTo(cacheLoader.checkKey(key));
        assertThat(cacheLoader.created()).isOne();

        // cache hit. Same value, no creation event.
        value = cache.getWithLoader(key, cacheLoader);
        assertThat(value).isEqualTo(cacheLoader.checkKey(key));
        assertThat(cacheLoader.created()).isOne();

        String key2 = UUID.randomUUID().toString();

        // cache creation. second creation event.
        String value2 = cache.getWithLoader(key2, cacheLoader);
        assertThat(cacheLoader.created()).isEqualTo(2);
        assertThat(value2).isEqualTo(cacheLoader.checkKey(key2));

        String key3 = UUID.randomUUID().toString();

        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() ->
                        cache.getWithLoader(key3, k3 -> {
                            throw new RuntimeException(key3);
                        }))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(key3);
        }
    }

    public static final class TestingCacheLoader implements JdbiCacheLoader<String, String> {

        private final Map<String, String> values = new HashMap<>();
        private int creations = 0;

        @Override
        public String create(String key) {
            String value = creations++ + "V_" + UUID.randomUUID();
            values.put(key, value);
            return value;
        }

        public int created() {
            return creations;
        }

        public String checkKey(String key) {
            return values.get(key);
        }
    }
}

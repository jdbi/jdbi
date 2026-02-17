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
package org.jdbi.cache.caffeine;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.jdbi.core.cache.JdbiCacheBuilder;
import org.jdbi.core.cache.internal.JdbiCacheTest;

class CaffeineCacheTest extends JdbiCacheTest {
    @Override
    protected JdbiCacheBuilder setupBuilder() {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder().expireAfterAccess(Duration.ofSeconds(5)).initialCapacity(10);
        return new CaffeineCacheBuilder(caffeine);
    }
}

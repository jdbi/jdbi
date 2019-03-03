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
package org.jdbi.v3.core.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jdbi.v3.meta.Beta;

/**
 * Hold metadata caches which maps various JVM constants into pre-parsed forms.
 * For example, bean property accessors, or normalized enum constants.
 * Note that unlike most JdbiConfig types, this cache is Jdbi level and shared,
 * so it should not hold data that needs to respect reconfiguration.
 * Additionally, currently there is no expiration policy, as nearly all keys are
 * references to JVM constant pool entries.
 * <b>This makes it unsuitable as a general-purpose shared cache.</b>
 */
@Beta
public final class JdbiCaches implements JdbiConfig<JdbiCaches> {
    private final Map<JdbiCache<?, ?>, Map<Object, Object>> caches = new ConcurrentHashMap<>();

    /**
     * Does not actually create a copy!!
     */
    @Override
    public JdbiCaches createCopy() {
        return this;
    }

    public static <K, V> JdbiCache<K, V> declare(Function<K, V> computer) {
        return declare(Function.identity(), computer);
    }

    public static <K, V> JdbiCache<K, V> declare(Function<K, ?> keyNormalizer, Function<K, V> computer) {
        return new JdbiCache<K, V>() {
            @SuppressWarnings("unchecked")
            @Override
            public V get(K key, ConfigRegistry config) {
                return (V) config.get(JdbiCaches.class).caches
                        .computeIfAbsent(this, x -> new ConcurrentHashMap<>())
                        .computeIfAbsent(keyNormalizer.apply(key), x -> computer.apply(key));
            }
        };
    }
}

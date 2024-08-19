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
package org.jdbi.v3.core.config.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;

/**
 * Hold metadata caches which maps various JVM constants into pre-parsed forms.
 * For example, bean property accessors, or normalized enum constants.
 * Note that unlike most JdbiConfig types, this cache is Jdbi level and shared,
 * so it should not hold data that needs to respect reconfiguration.
 * Additionally, currently there is no expiration policy, as nearly all keys are
 * references to JVM constant pool entries.
 * <b>This makes it unsuitable as a general-purpose shared cache.</b>
 */
public final class ConfigCaches implements JdbiConfig<ConfigCaches> {

    private final Map<ConfigCache<?, ?>, Map<Object, Object>> caches = new ConcurrentHashMap<>();

    /**
     * Does not actually create a copy!!
     */
    @Override
    public ConfigCaches createCopy() {
        return this;
    }

    public static <K, V> ConfigCache<K, V> declare(Function<K, V> computer) {
        return declare(Function.identity(), computer);
    }

    public static <K, V> ConfigCache<K, V> declare(Function<K, ?> keyNormalizer, Function<K, V> computer) {
        return declare(keyNormalizer, (config, k) -> computer.apply(k));
    }

    public static <K, V> ConfigCache<K, V> declare(BiFunction<ConfigRegistry, K, V> computer) {
        return declare(Function.identity(), computer);
    }

    public static <K, V> ConfigCache<K, V> declare(Function<K, ?> keyNormalizer, BiFunction<ConfigRegistry, K, V> computer) {
        return new ConfigCache<>() {
            @SuppressWarnings("unchecked")
            @Override
            public V get(K key, ConfigRegistry config) {
                return (V) config.get(ConfigCaches.class).caches
                    .computeIfAbsent(this, x -> new ConcurrentHashMap<>())
                    .computeIfAbsent(keyNormalizer.apply(key), x -> computer.apply(config, key));
            }
        };
    }
}

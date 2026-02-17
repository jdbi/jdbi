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
package org.jdbi.core.config.internal;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.config.Configurable;
import org.jdbi.core.statement.StatementContext;

/**
 * Simple cache interface.
 *
 * @see ConfigCaches
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ConfigCache<K, V> {

    V get(K key, ConfigRegistry config);

    default V get(K key, Configurable<?> configurable) {
        return get(key, configurable.getConfig());
    }

    default V get(K key, StatementContext ctx) {
        return get(key, ctx.getConfig());
    }
}

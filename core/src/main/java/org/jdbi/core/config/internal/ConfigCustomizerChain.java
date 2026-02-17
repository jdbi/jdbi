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

import java.util.LinkedHashSet;
import java.util.Set;

import org.jdbi.core.config.ConfigCustomizer;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.meta.Beta;

/**
 * Applies a set of {@link ConfigCustomizer}s sequentially to a {@link ConfigRegistry} object.
 *
 * @since 3.38.0
 */
@Beta
public final class ConfigCustomizerChain implements ConfigCustomizer {

    private final Set<ConfigCustomizer> configCustomizers = new LinkedHashSet<>();

    /**
     * Adds a customizer to the end of the chain.
     * @param configCustomizer A {@link ConfigCustomizer} instance. Must not be null
     */
    public void addCustomizer(final ConfigCustomizer configCustomizer) {
        configCustomizers.add(configCustomizer);
    }

    /**
     * Applies all customizers in the chain to the given {@link ConfigRegistry} object.
     * @param config A {@link ConfigRegistry} object
     */
    @Override
    public void customize(final ConfigRegistry config) {
        configCustomizers.forEach(configCustomizer -> configCustomizer.customize(config));
    }
}

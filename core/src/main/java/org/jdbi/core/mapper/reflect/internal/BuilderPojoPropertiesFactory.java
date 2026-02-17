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
package org.jdbi.core.mapper.reflect.internal;

import java.util.function.Supplier;

import org.jdbi.core.config.internal.ConfigCache;
import org.jdbi.core.config.internal.ConfigCaches;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface BuilderPojoPropertiesFactory extends PojoPropertiesFactory {
    ConfigCache<BuilderSpec<?, ?>, BuilderPojoProperties<?, ?>> BUILDER_CACHE =
        ConfigCaches.declare(s -> s.type, BuilderPojoProperties::new);

    static <T, B> PojoPropertiesFactory builder(Class<T> defn, Supplier<B> builder) {
        return (t, config) -> BUILDER_CACHE.get(new BuilderSpec<>(t, config, defn, builder), config);
    }
}

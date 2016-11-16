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
package org.jdbi.v3.core;

import java.util.function.Consumer;

public interface Configurable<This> {
    /**
     * Returns the configuration registry associated with this object.
     * @return the configuration registry associated with this object.
     */
    ConfigRegistry getConfig();

    /**
     * Gets the configuration object of the given type, associated with this object.
     * @param configClass the configuration type
     * @param <C> the configuration type
     * @return the configuration object of the given type, associated with this object.
     */
    default <C extends JdbiConfig<C>> C getConfig(Class<C> configClass) {
        return getConfig().get(configClass);
    }

    /**
     * Passes the configuration object of the given type to the configurer, then returns this object.
     * @param configClass the configuration type
     * @param configurer consumer that will be passed the configuration object
     * @param <C> the configuration type
     * @return this object (for call chaining)
     */
    @SuppressWarnings("unchecked")
    default <C extends JdbiConfig<C>> This configure(Class<C> configClass, Consumer<C> configurer) {
        configurer.accept(getConfig(configClass));
        return (This) this;
    }
}

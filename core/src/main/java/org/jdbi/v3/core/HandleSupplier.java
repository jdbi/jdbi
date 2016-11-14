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

import java.util.concurrent.Callable;

/**
 * A handle supplier for extension implementors.
 */
public interface HandleSupplier {
    /**
     * Returns the extension method currently being called with this handle.
     */
    ExtensionMethod getExtensionMethod();

    /**
     * Sets the extension method currently being called with this handle.
     *
     * @param extensionMethod the extension method
     */
    void setExtensionMethod(ExtensionMethod extensionMethod);

    ConfigRegistry getConfig();

    <V> V withConfig(ConfigRegistry config, Callable<V> task) throws Exception;

    default <C extends JdbiConfig<C>> C getConfig(Class<C> configClass) {
        return getConfig().get(configClass);
    }

    /**
     * Returns a handle, possibly creating it lazily. A Handle holds a database connection, so extensions should only
     * call this method in order to interact with the database.
     */
    Handle getHandle();
}

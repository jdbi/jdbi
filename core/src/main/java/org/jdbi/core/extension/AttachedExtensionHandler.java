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
package org.jdbi.core.extension;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.meta.Alpha;
import org.jdbi.meta.Beta;

/**
 * Extension handler with a target object attached.
 */
@FunctionalInterface
@Alpha
public interface AttachedExtensionHandler {
    /**
     * Gets invoked to return a value for the method that this handler was is handling, on the attached target instance.
     * @param handleSupplier A {@link HandleSupplier} instance for accessing the handle and its related objects
     * @param args Optional arguments for the handler
     * @return The return value for the method that was bound to the extension handler. Can be null
     * @throws Exception Any exception from the underlying code
     */
    Object invoke(HandleSupplier handleSupplier, Object... args) throws Exception;

    /**
     * Called after the method handler is constructed to pre-initialize any important
     * configuration data structures.
     *
     * @param config the method configuration to use for warming up
     */
    @Beta
    default void warm(ConfigRegistry config) {}
}

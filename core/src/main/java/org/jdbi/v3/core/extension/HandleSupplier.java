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
package org.jdbi.v3.core.extension;

import java.util.concurrent.Callable;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.Configurable;

/**
 * A handle supplier for extension implementors.
 */
public interface HandleSupplier extends Configurable<HandleSupplier> {
    /**
     * Returns a handle, possibly creating it lazily. A Handle holds a database connection, so extensions should only
     * call this method in order to interact with the database.
     *
     * @return an open Handle
     */
    Handle getHandle();

    /**
     * @return the owning Jdbi instance
     */
    Jdbi getJdbi();

    /**
     * Bind an extension method and configuration registry to the Handle,
     * invoke the given task, then reset the Handle's extension state.
     *
     * Note that the binding is done by a thread local, so the binding
     * will not propagate to other threads you may call out to.
     *
     * @param <V> the result type of the task
     * @param extensionMethod the method invoked
     * @param config the configuration registry
     * @param task the code to execute in an extension context
     * @return the callable's result
     * @throws Exception if any exception is thrown
     */
    <V> V invokeInContext(ExtensionMethod extensionMethod, ConfigRegistry config, Callable<V> task) throws Exception;
}

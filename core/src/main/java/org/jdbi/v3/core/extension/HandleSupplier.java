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

/**
 * A handle supplier for extension implementors.
 */
public interface HandleSupplier extends AutoCloseable {

    /**
     * Returns a handle, possibly creating it lazily. A Handle holds a database connection, so extensions should only
     * call this method in order to interact with the database.
     *
     * @return An open Handle.
     */
    Handle getHandle();

    /**
     * Returns the owning Jdbi instance.
     *
     * @return The owning Jdbi instance.
     */
    Jdbi getJdbi();

    /**
     * Returns the current Jdbi config.
     *
     * @return The current Jdbi configuration.
     */
    ConfigRegistry getConfig();

    /**
     * Bind a new {@link ExtensionContext} to the Handle, invoke the given task, then restore the Handle's extension state.
     *
     * @param <V>              the result type of the task
     * @param extensionContext An {@link ExtensionContext} object that manages the extension state.
     * @param task             the code to execute in an extension context
     * @return the callable's result
     * @throws Exception if any exception is thrown
     */
    <V> V invokeInContext(ExtensionContext extensionContext, Callable<V> task) throws Exception;

    @Override
    default void close() {}
}

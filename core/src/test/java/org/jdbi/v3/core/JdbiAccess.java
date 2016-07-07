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

import org.jdbi.v3.core.transaction.LocalTransactionHandler;

/**
 * Utilities for testing jdbi internal classes.
 */
public class JdbiAccess {
    /**
     * Create a handle with no connection,
     * useful for tests that do not actually hit
     * a database.
     */
    public static Handle createHandle() {
        return new Handle(new JdbiConfig(), new LocalTransactionHandler(),
                new DefaultStatementBuilder(), null);
    }

    /**
     * Create a simple statement context that shares configuration
     * with the given handle.
     */
    public static StatementContext createContext(Handle handle) {
        return new StatementContext(handle.config);
    }
}

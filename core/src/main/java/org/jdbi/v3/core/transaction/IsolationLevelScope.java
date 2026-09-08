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
package org.jdbi.v3.core.transaction;

import org.jdbi.v3.core.Handle;

/**
 * Applies an isolation level to a handle and restores the previous level on close. A failure to
 * restore is attached to an exception that is already in flight instead of replacing it, which is
 * why this is a resource rather than a finally block.
 */
final class IsolationLevelScope implements AutoCloseable {

    private final Handle handle;
    private final TransactionIsolationLevel previousLevel;

    IsolationLevelScope(Handle handle, TransactionIsolationLevel level) {
        this.handle = handle;
        this.previousLevel = handle.getTransactionIsolationLevel();
        handle.setTransactionIsolationLevel(level);
    }

    @Override
    public void close() {
        handle.setTransactionIsolationLevel(previousLevel);
    }
}

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
 * A transaction handler that always calls {@link #rollback(org.jdbi.v3.core.Handle)} instead
 * of {@link #commit(org.jdbi.v3.core.Handle)}.
 */
public class RollbackOnlyTransactionHandler extends DelegatingTransactionHandler {
    public RollbackOnlyTransactionHandler() {
        this(new LocalTransactionHandler());
    }

    public RollbackOnlyTransactionHandler(final TransactionHandler delegate) {
        super(delegate);
    }

    @Override
    public void commit(final Handle handle) {
        rollback(handle);
    }
}

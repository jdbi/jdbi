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

import java.sql.SQLException;

import org.jdbi.v3.core.Handle;

/**
 * Handler designed to behave properly in a J2EE CMT environment. It will never
 * explicitly begin or commit a transaction, and will throw a runtime exception
 * when rollback is called to force rollback.
 * <p>
 * Unlike its base class {@link NoOpTransactionHandler}, this handler reads the
 * autocommit flag of the connection: when autocommit is disabled, the handle
 * reports an open transaction. Use {@link NoOpTransactionHandler} instead when
 * the connection is wrapped by a proxy on which the autocommit flag does not
 * show the real transaction state.
 * </p>
 */
public class CMTTransactionHandler extends NoOpTransactionHandler {
    /**
     * Called to test if a handle is in a transaction
     */
    @Override
    public boolean isInTransaction(Handle handle) {
        try {
            return !handle.getConnection().getAutoCommit();
        } catch (SQLException e) {
            throw new TransactionException("Failed to check status of transaction", e);
        }
    }
}

/*
 * Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.Handle;

/**
 * Interface which defines callbacks to be used when transaction methods are called on a handle.
 * Used by specifying on an <code>IDBI</code> instance. All <code>Handle</code> instances
 * opened from that <code>IDBI</code> will use the handler specified.
 * <p />
 * The default implementation, <code>ConnectionTransactionHandler</code>, explicitely manages
 * the transactions on the underlying JDBC <code>Connection</code>.
 */
public interface TransactionHandler
{
    /**
     * Called when a transaction is started
     */
    public void begin(Handle handle); // TODO consider having this return a TransactionStatus

    /**
     * Called when a transaction is committed
     */
    public void commit(Handle handle);

    /**
     * Called when a transaction is rolled back
     */
    public void rollback(Handle handle);

    /**
     * Called to test if a handle is in a transaction
     */
    public boolean isInTransaction(Handle handle);
}

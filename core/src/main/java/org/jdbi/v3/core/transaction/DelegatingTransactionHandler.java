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
import org.jdbi.v3.core.HandleCallback;

public class DelegatingTransactionHandler implements TransactionHandler
{
    private final TransactionHandler delegate;

    public DelegatingTransactionHandler(TransactionHandler delegate)
    {
        this.delegate = delegate;
    }

    protected TransactionHandler getDelegate()
    {
        return delegate;
    }

    @Override
    public void begin(Handle handle)
    {
        delegate.begin(handle);
    }

    @Override
    public void commit(Handle handle)
    {
        delegate.commit(handle);
    }

    @Override
    public void rollback(Handle handle)
    {
        delegate.rollback(handle);
    }

    @Override
    public void rollbackToSavepoint(Handle handle, String name)
    {
        delegate.rollbackToSavepoint(handle, name);
    }

    @Override
    public boolean isInTransaction(Handle handle)
    {
        return delegate.isInTransaction(handle);
    }

    @Override
    public void savepoint(Handle handle, String name)
    {
        delegate.savepoint(handle, name);
    }

    @Override
    public void releaseSavepoint(Handle handle, String name)
    {
        delegate.releaseSavepoint(handle, name);
    }

    @Override
    public <R, X extends Exception> R inTransaction(Handle handle,
                                                    HandleCallback<R, X> callback) throws X
    {
        return delegate.inTransaction(handle, callback);
    }

    @Override
    public <R, X extends Exception> R inTransaction(Handle handle,
                                                    TransactionIsolationLevel level,
                                                    HandleCallback<R, X> callback) throws X
    {
        return delegate.inTransaction(handle, level, callback);
    }
}

/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
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
package org.skife.jdbi.v2.tweak.transactions;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.tweak.TransactionHandler;

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
    public void rollback(Handle handle, String name)
    {
        delegate.rollback(handle, name);
    }

    @Override
    public boolean isInTransaction(Handle handle)
    {
        return delegate.isInTransaction(handle);
    }

    @Override
    public void checkpoint(Handle handle, String name)
    {
        delegate.checkpoint(handle, name);
    }

    @Override
    public void release(Handle handle, String checkpointName)
    {
        delegate.release(handle, checkpointName);
    }

    @Override
    public <ReturnType> ReturnType inTransaction(Handle handle, TransactionCallback<ReturnType> callback)
    {
        return delegate.inTransaction(handle, callback);
    }

    @Override
    public <ReturnType> ReturnType inTransaction(Handle handle, TransactionIsolationLevel level, TransactionCallback<ReturnType> callback)
    {
        return delegate.inTransaction(handle, level, callback);
    }
}

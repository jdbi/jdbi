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

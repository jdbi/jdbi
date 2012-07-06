package org.skife.jdbi.v2.tweak.transactions;

import java.util.concurrent.atomic.AtomicBoolean;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.exceptions.TransactionFailedException;
import org.skife.jdbi.v2.tweak.TransactionRunner;

public class SimpleTransactionRunner implements TransactionRunner
{

    @Override
    public <ReturnType> ReturnType inTransaction(Handle handle, TransactionCallback<ReturnType> callback)
    {
        final AtomicBoolean failed = new AtomicBoolean(false);
        TransactionStatus status = new TransactionStatus()
        {
            @Override
            public void setRollbackOnly()
            {
                failed.set(true);
            }
        };
        final ReturnType returnValue;
        try {
            handle.begin();
            returnValue = callback.inTransaction(handle, status);
            if (!failed.get()) {
                handle.commit();
            }
        }
        catch (RuntimeException e) {
            handle.rollback();
            throw e;
        }
        catch (Exception e) {
            handle.rollback();
            throw new TransactionFailedException("Transaction failed do to exception being thrown " +
                                                 "from within the callback. See cause " +
                                                 "for the original exception.", e);
        }
        if (failed.get()) {
            handle.rollback();
            throw new TransactionFailedException("Transaction failed due to transaction status being set " +
                                                 "to rollback only.");
        }
        else {
            return returnValue;
        }
    }

    @Override
    public <ReturnType> ReturnType inTransaction(Handle handle, TransactionIsolationLevel level,
            TransactionCallback<ReturnType> callback)
    {
        final TransactionIsolationLevel initial = handle.getTransactionIsolationLevel();
        try {
            handle.setTransactionIsolation(level);
            return inTransaction(handle, callback);
        }
        finally {
            handle.setTransactionIsolation(initial);
        }
    }
}

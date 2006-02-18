package org.skife.jdbi.v2.tweak.transactions;

import org.skife.jdbi.v2.exceptions.TransactionException;
import org.skife.jdbi.v2.tweak.TransactionHandler;
import org.skife.jdbi.v2.Handle;

import java.sql.SQLException;

/**
 * Handler designed to behave properly in a J2EE CMT environment. It will never
 * explicitely begin or commit a transaction, and will throw a runtime exception
 * when rollback is called to force rollback.
 */
public class CMTTransactionHandler implements TransactionHandler
{
    /**
     * Called when a transaction is started
     */
    public void begin(Handle handle)
    {
        // noop
    }

    /**
     * Called when a transaction is committed
     */
    public void commit(Handle handle)
    {
        // noop
    }

    /**
     * Called when a transaction is rolled back
     * Will throw a RuntimeException to force transactional rollback
     */
    public void rollback(Handle handle)
    {
        throw new TransactionException("Rollback called, this runtime exception thrown to halt the transaction");
    }

    /**
     * Called to test if a handle is in a transaction
     */
    public boolean isInTransaction(Handle handle)
    {
        try
        {
            return ! handle.getConnection().getAutoCommit();
        }
        catch (SQLException e)
        {
            throw new TransactionException("Failed to check status of transaction", e);
        }
    }
}

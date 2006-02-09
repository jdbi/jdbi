package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.TransactionException;
import org.skife.jdbi.v2.tweak.TransactionHandler;

import java.sql.SQLException;

/**
 * 
 */
public class LocalTransactionHandler implements TransactionHandler
{
    /**
     * Called when a transaction is started
     */
    public void begin(Handle handle)
    {
        try
        {
            handle.getConnection().setAutoCommit(false);
        }
        catch (SQLException e)
        {
            throw new TransactionException("Failed to start transaction", e);
        }
    }

    /**
     * Called when a transaction is committed
     */
    public void commit(Handle handle)
    {
        try
        {
            handle.getConnection().commit();
        }
        catch (SQLException e)
        {
            throw new TransactionException("Failed to commit transaction", e);
        }
    }

    /**
     * Called when a transaction is rolled back
     */
    public void rollback(Handle handle)
    {
        try
        {
            handle.getConnection().rollback();
        }
        catch (SQLException e)
        {
            throw new TransactionException("Failed to rollback transaction", e);
        }
    }

    /**
     * Called to test if a handle is in a transaction
     */
    public boolean isInTransaction(Handle handle)
    {
        try
        {
            return !handle.getConnection().getAutoCommit();
        }
        catch (SQLException e)
        {
            throw new TransactionException("Failed to test for transaction status", e);
        }
    }
}

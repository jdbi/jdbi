package org.skife.jdbi.v2.tweak.transactions;

import java.sql.SQLException;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.tweak.TransactionHandler;

/**
 * A TransactionHandler that automatically retries transactions that fail due to
 * serialization failures, which can generally be resolved by automatically
 * retrying the transaction.  Any TransactionCallback used under this runner
 * should be aware that it may be invoked multiple times.
 */
public class SerializableTransactionRunner extends DelegatingTransactionHandler implements TransactionHandler
{
    /* http://www.postgresql.org/docs/9.1/static/errcodes-appendix.html */
    private static final String SQLSTATE_TXN_SERIALIZATION_FAILED = "40001";

    private final Configuration configuration;

    public SerializableTransactionRunner()
    {
        this(new Configuration(), new LocalTransactionHandler());
    }

    public SerializableTransactionRunner(Configuration configuration, TransactionHandler delegate)
    {
        super(delegate);
        this.configuration = configuration;
    }

    @Override
    public <ReturnType> ReturnType inTransaction(Handle handle, TransactionCallback<ReturnType> callback)
    {
        int retriesRemaining = configuration.maxRetries;

        while (true) {
            try
            {
                return getDelegate().inTransaction(handle, callback);
            } catch (Exception e)
            {
                if (!isSqlState(configuration.serializationFailureSqlState, e) || --retriesRemaining <= 0)
                {

                    if (e instanceof RuntimeException)
                    {
                        throw (RuntimeException) e;
                    }
                    throw handle.getExceptionPolicy().transactionFailed(e);
                }
            }
        }
    }

    @Override
    public <ReturnType> ReturnType inTransaction(Handle handle, TransactionIsolationLevel level,
            TransactionCallback<ReturnType> callback)
    {
        final TransactionIsolationLevel initial = handle.getTransactionIsolationLevel();
        try
        {
            handle.setTransactionIsolation(level);
            return inTransaction(handle, callback);
        }
        finally
        {
            handle.setTransactionIsolation(initial);
        }
    }

    /**
     * Returns true iff the Throwable or one of its causes is an SQLException whose SQLState begins
     * with the passed state.
     */
    protected boolean isSqlState(String expectedSqlState, Throwable throwable)
    {
        do
        {
            if (throwable instanceof SQLException)
            {
                String sqlState = ((SQLException) throwable).getSQLState();

                if (sqlState != null && sqlState.startsWith(expectedSqlState))
                {
                    return true;
                }
            }
        } while ( (throwable = throwable.getCause()) != null);

        return false;
    }

    public static class Configuration
    {
        private final int maxRetries;
        private final String serializationFailureSqlState;

        public Configuration()
        {
            this(5, SQLSTATE_TXN_SERIALIZATION_FAILED);
        }

        private Configuration(int maxRetries, String serializationFailureSqlState)
        {
            this.maxRetries = maxRetries;
            this.serializationFailureSqlState = serializationFailureSqlState;
        }

        public Configuration withMaxRetries(int maxRetries)
        {
            return new Configuration(maxRetries, serializationFailureSqlState);
        }

        public Configuration withSerializationFailureSqlState(String serializationFailureSqlState)
        {
            return new Configuration(maxRetries, serializationFailureSqlState);
        }
    }
}

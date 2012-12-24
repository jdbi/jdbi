package org.skife.jdbi.v2.exceptions;

import org.skife.jdbi.v2.StatementContext;

/**
 * Controls the exceptions that jDBI throws in response to various failures.
 * Most of these are not interesting to override, but in particular
 * {@link #unableToExecuteStatement(String, Throwable, StatementContext)}
 * would let you give more detailed responses to {@link java.sql.SQLException}s.
 */
public class ExceptionPolicy
{
    public DBIException unableToOpenConnection(Throwable cause)
    {
        throw new UnableToObtainConnectionException(cause);
    }

    public final DBIException unableToExecuteStatement(Throwable cause, StatementContext ctx)
    {
        throw unableToExecuteStatement(null, cause, ctx);
    }

    public final DBIException unableToExecuteStatement(String msg, StatementContext ctx)
    {
        throw unableToExecuteStatement(msg, null, ctx);
    }

    public DBIException unableToExecuteStatement(String msg, Throwable cause, StatementContext ctx)
    {
        throw new UnableToExecuteStatementException(msg, cause, ctx);
    }

    public final DBIException unableToCreateStatement(Throwable cause, StatementContext ctx)
    {
        throw unableToCreateStatement(null, cause, ctx);
    }

    public DBIException unableToCreateStatement(String msg, Throwable cause, StatementContext ctx)
    {
        throw new UnableToCreateStatementException(msg, cause, ctx);
    }

    public DBIException unableToCloseResource(String msg, Throwable cause)
    {
        throw new UnableToCloseResourceException(msg, cause);
    }

    public DBIException transactionFailed(String msg, Throwable cause)
    {
        throw new TransactionFailedException(msg, cause);
    }

    public final DBIException transactionFailed(Throwable cause)
    {
        throw transactionFailed(null, cause);
    }

    public final DBIException transactionFailed(String msg)
    {
        throw transactionFailed(msg, null);
    }

    public DBIException transactionException(String msg, Throwable cause)
    {
        throw new TransactionException(msg, cause);
    }

    public final DBIException transactionException(Throwable cause)
    {
        throw transactionException(null, cause);
    }

    public final DBIException transactionException(String msg)
    {
        throw transactionException(msg, null);
    }

    public DBIException callbackFailed(String msg, Throwable cause)
    {
        throw new CallbackFailedException(msg, cause);
    }

    public final DBIException callbackFailed(Throwable cause)
    {
        throw callbackFailed(null, cause);
    }

    public DBIException resultSetFailure(String msg, Exception e, StatementContext ctx)
    {
        throw new ResultSetException(msg, e, ctx);
    }

    public final DBIException unableToSetTransactionIsolation(int i, Throwable cause)
    {
        throw unableToSetTransactionIsolation("Unable to set isolation level to " + i, cause);
    }

    public DBIException unableToSetTransactionIsolation(String msg, Throwable cause)
    {
        throw new UnableToManipulateTransactionIsolationLevelException(msg, cause);
    }
}

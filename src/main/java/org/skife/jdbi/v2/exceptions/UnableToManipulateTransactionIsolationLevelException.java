package org.skife.jdbi.v2.exceptions;

public class UnableToManipulateTransactionIsolationLevelException extends DBIException
{
    public UnableToManipulateTransactionIsolationLevelException(int i, Throwable cause)
    {
        super("Unable to set isolation level to " + i, cause);
    }

    public UnableToManipulateTransactionIsolationLevelException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}

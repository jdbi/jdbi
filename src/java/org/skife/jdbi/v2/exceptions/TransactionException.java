package org.skife.jdbi.v2.exceptions;

/**
 * 
 */
public class TransactionException extends DBIException
{
    public TransactionException(String string, Throwable throwable)
    {
        super(string, throwable);
    }

    public TransactionException(Throwable cause)
    {
        super(cause);
    }
}

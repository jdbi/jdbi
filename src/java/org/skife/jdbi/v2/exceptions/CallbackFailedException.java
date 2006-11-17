package org.skife.jdbi.v2.exceptions;

/**
 * Exception used to indicate an exception thrown during a provided callback. The wrapped
 * throwable will be the client library thrown checked exception.
 */
public class CallbackFailedException extends DBIException
{
    public CallbackFailedException(String string, Throwable throwable)
    {
        super(string, throwable);
    }

    public CallbackFailedException(Throwable cause)
    {
        super(cause);
    }
}

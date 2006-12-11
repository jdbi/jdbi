package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.TransactionHandler;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;

/**
 * An interface for {@link DBI} instances for systems which like
 * to work with interfaces.
 */
public interface IDBI
{
    /**
     * Obtain a Handle to the data source wrapped by this DBI instance
     *
     * @return an open Handle instance
     *
     * @see org.skife.jdbi.v2.DBI#open()
     */
    Handle open();

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients.
     *
     * @param callback A callback which will receive an open Handle
     *
     * @return the value returned by callback
     *
     * @throws CallbackFailedException Will be thrown if callback raises an exception. This exception will
     *                                 wrap the exception thrown by the callback.
     */
    <ReturnType> ReturnType withHandle(HandleCallback<ReturnType> callback) throws CallbackFailedException;
}

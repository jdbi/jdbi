package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.Handle;

/**
 * Callback for use with {@link org.skife.jdbi.v2.DBI#withHandle(Object)}
 */
public interface HandleCallback<T>
{
    /**
     * Will be invoked with an open Handle. The handle will be closed when this
     * callback returns. Any exception thrown will be wrapped in a
     * {@link org.skife.jdbi.v2.exceptions.CallbackFailedException}
     *
     * @param handle Handle to be used only within scope of this callback
     * @return The return value of the callback
     * @throws Exception will result in a {@link org.skife.jdbi.v2.exceptions.CallbackFailedException} wrapping
     *                   the exception being thrown 
     */
    public T withHandle(Handle handle) throws Exception;
}

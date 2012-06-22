package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.Handle;

/**
 * Abstract {@link HandleCallback} that doesn't return a result.
 */
public abstract class VoidHandleCallback implements HandleCallback<Void>
{
    /**
     * This implementation delegates to {@link #execute}.
     *
     * @param handle {@inheritDoc}
     * @return nothing
     * @throws Exception {@inheritDoc}
     */
    @Override
    public final Void withHandle(Handle handle) throws Exception
    {
        execute(handle);
        return null;
    }

    /**
     * {@link #withHandle} will delegate to this method.
     *
     * @param handle Handle to be used only within scope of this callback
     * @throws Exception will result in a {@link org.skife.jdbi.v2.exceptions.CallbackFailedException} wrapping
     *                   the exception being thrown
     */
    protected abstract void execute(Handle handle) throws Exception;
}

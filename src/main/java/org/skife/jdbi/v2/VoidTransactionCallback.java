package org.skife.jdbi.v2;

/**
 * Abstract {@link TransactionCallback} that doesn't return a result.
 */
public abstract class VoidTransactionCallback implements TransactionCallback<Void>
{
    /**
     * This implementation delegates to {@link #execute}.
     *
     * @param handle {@inheritDoc}
     * @return nothing
     * @throws Exception {@inheritDoc}
     */
    @Override
    public final Void inTransaction(Handle handle, TransactionStatus status) throws Exception
    {
        execute(handle, status);
        return null;
    }

    /**
     * {@link #inTransaction} will delegate to this method.
     *
     * @param handle Handle to be used only within scope of this callback
     * @param status Allows rolling back the transaction
     * @throws Exception will result in a {@link org.skife.jdbi.v2.exceptions.CallbackFailedException} wrapping
     *                   the exception being thrown
     */
    protected abstract void execute(Handle handle, TransactionStatus status) throws Exception;
}

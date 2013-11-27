/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v3;

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
     * @throws Exception will result in a {@link org.skife.jdbi.v3.exceptions.CallbackFailedException} wrapping
     *                   the exception being thrown
     */
    protected abstract void execute(Handle handle, TransactionStatus status) throws Exception;
}

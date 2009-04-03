/*
 * Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.tweak.HandleCallback;

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
     * Define an attribute on every {@link StatementContext} for every statement created
     * from a handle obtained from this DBI instance.
     *
     * @param key The key for the attribute
     * @param value the value for the attribute
     */
    void define(String key, Object value);

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

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients. The handle will be in a transaction when the callback is invoked, and
     * that transaction will be committed if the callback finishes normally, or rolled back if the
     * callback raises an exception.
     *
     * @param callback A callback which will receive an open Handle, in a transaction
     *
     * @return the value returned by callback
     *
     * @throws CallbackFailedException Will be thrown if callback raises an exception. This exception will
     *                                 wrap the exception thrown by the callback.
     */
    <ReturnType> ReturnType inTransaction(TransactionCallback<ReturnType> callback) throws CallbackFailedException;

}

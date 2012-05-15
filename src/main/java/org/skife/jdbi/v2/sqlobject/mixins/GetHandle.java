/*
 * Copyright 2004 - 2011 Brian McCallister
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

package org.skife.jdbi.v2.sqlobject.mixins;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.tweak.HandleCallback;

/**
 * A mixin interface to define a method for obtaining the Handle attached to a sql object
 */
public interface GetHandle
{
    /**
     * Obtain the handle associated to this sql object.
     *
     * @return the handle
     */
    public Handle getHandle();

    /**
     * A convenience function which manages the lifecycle of the handle associated to this sql object,
     * and yields it to a callback for use by clients.
     *
     * @param callback A callback which will receive the handle associated to this sql object
     *
     * @return the value returned by callback
     *
     * @throws CallbackFailedException Will be thrown if callback raises an exception. This exception will
     *                                 wrap the exception thrown by the callback.
     */
    public <ReturnType> ReturnType withHandle(HandleCallback<ReturnType> callback) throws CallbackFailedException;
}

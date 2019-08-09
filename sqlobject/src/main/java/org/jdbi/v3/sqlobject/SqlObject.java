/*
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
package org.jdbi.v3.sqlobject;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;

/**
 * SqlObject base interface.  All synthesized implementations implement
 * this interface, even if the declaration doesn't extend {@code SqlObject}.
 *
 * Custom mixins may be written as subinterfaces of this class with default methods.
 */
public interface SqlObject {
    /**
     * @return the handle open in the current sql object context.
     */
    Handle getHandle();

    /**
     * A convenience function which manages the lifecycle of the handle associated to this sql object,
     * and yields it to a callback for use by clients.
     *
     * @param callback A callback which will receive the handle associated to this sql object
     * @param <R> type returned by the callback
     * @param <X> exception type thrown by the callback, if any.
     *
     * @return the value returned by callback
     *
     * @throws X any exception thrown by the callback
     */
    <R, X extends Exception> R withHandle(HandleCallback<R, X> callback) throws X;

    /**
     * A convenience function which manages the lifecycle of the handle associated to this sql object,
     * and yields it to a consumer for use by clients.
     *
     * @param consumer A consumer which will receive the handle associated to this sql object
     * @param <X> exception type thrown by the callback, if any.
     *
     * @throws X any exception thrown by the callback
     */
    default <X extends Exception> void useHandle(HandleConsumer<X> consumer) throws X {
        withHandle(consumer.asCallback());
    }
}

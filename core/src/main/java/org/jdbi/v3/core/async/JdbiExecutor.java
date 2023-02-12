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
package org.jdbi.v3.core.async;

import java.util.concurrent.CompletionStage;

import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.extension.ExtensionCallback;
import org.jdbi.v3.core.extension.ExtensionConsumer;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

public interface JdbiExecutor {

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients.
     *
     * <p>
     * The callback will be executed in a separate thread
     * </p>
     *
     * @param callback A callback which will receive an open Handle
     * @param <R> type returned by the callback
     * @param <X> exception type thrown by the callback, if any.
     *
     * @return a completion stage which completes when the callback returns a value or throws an exception
     */
    <R, X extends Exception> CompletionStage<R> withHandle(HandleCallback<R, X> callback);

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients. The handle will be in a transaction when the callback is invoked, and
     * that transaction will be committed if the callback finishes normally, or rolled back if the
     * callback raises an exception.
     *
     * <p>
     * The callback will be executed in a separate thread
     * </p>
     *
     * @param callback A callback which will receive an open Handle, in a transaction
     * @param <R> type returned by the callback
     * @param <X> exception type thrown by the callback, if any.
     *
     * @return a completion stage which completes when the callback returns a value or throws an exception
     */
    <R, X extends Exception> CompletionStage<R> inTransaction(HandleCallback<R, X> callback);

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients. The handle will be in a transaction when the callback is invoked, and
     * that transaction will be committed if the callback finishes normally, or rolled back if the
     * callback raises an exception.
     *
     * <p>
     * This form accepts a transaction isolation level which will be applied to the connection
     * for the scope of this transaction, after which the original isolation level will be restored.
     * </p>
     *
     * <p>
     * The callback will be executed in a separate thread
     * </p>
     *
     * @param level the transaction isolation level which will be applied to the connection for the scope of this
     *              transaction, after which the original isolation level will be restored.
     * @param callback A callback which will receive an open Handle, in a transaction
     * @param <R> type returned by the callback
     * @param <X> exception type thrown by the callback, if any.
     *
     * @return a completion stage which completes when the callback returns a value or throws an exception
     */
    <R, X extends Exception> CompletionStage<R> inTransaction(TransactionIsolationLevel level, HandleCallback<R, X> callback);

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients.
     *
     * <p>
     * The callback will be executed in a separate thread
     * </p>
     *
     * @param consumer A callback which will receive an open Handle
     * @param <X> exception type thrown by the callback, if any.
     *
     * @return a completion stage which completes when the callback returns or throws an exception
     */
    <X extends Exception> CompletionStage<Void> useHandle(HandleConsumer<X> consumer);

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients. The handle will be in a transaction when the callback is invoked, and
     * that transaction will be committed if the callback finishes normally, or rolled back if the
     * callback raises an exception.
     *
     * <p>
     * The callback will be executed in a separate thread
     * </p>
     *
     * @param callback A callback which will receive an open Handle, in a transaction
     * @param <X> exception type thrown by the callback, if any.
     *
     * @return a completion stage which completes when the callback returns or throws an exception
     */
    <X extends Exception> CompletionStage<Void> useTransaction(HandleConsumer<X> callback);

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients. The handle will be in a transaction when the callback is invoked, and
     * that transaction will be committed if the callback finishes normally, or rolled back if the
     * callback raises an exception.
     *
     * <p>
     * This form accepts a transaction isolation level which will be applied to the connection
     * for the scope of this transaction, after which the original isolation level will be restored.
     * </p>
     *
     * <p>
     * The callback will be executed in a separate thread
     * </p>
     *
     * @param level the transaction isolation level which will be applied to the connection for the scope of this
     *              transaction, after which the original isolation level will be restored.
     * @param callback A callback which will receive an open Handle, in a transaction
     * @param <X> exception type thrown by the callback, if any.
     *
     * @return a completion stage which completes when the callback returns or throws an exception
     */
    <X extends Exception> CompletionStage<Void> useTransaction(TransactionIsolationLevel level, HandleConsumer<X> callback);

    /**
     * A convenience method which opens an extension of the given type, yields it to a callback, and returns the result
     * of the callback. A handle is opened if needed by the extension, and closed before returning to the caller.
     *
     * <p>
     * The callback will be executed in a separate thread
     * </p>
     *
     * @param extensionType the type of extension.
     * @param callback      a callback which will receive the extension.
     * @param <R> the return type
     * @param <E> the extension type
     * @param <X> the exception type optionally thrown by the callback
     * @return a completion stage which completes when the callback returns a value
     * or throws an exception, or will complete with NoSuchExtensionException if no
     * {@link ExtensionFactory} is registered which supports the given extension type.
     */
    <R, E, X extends Exception> CompletionStage<R> withExtension(Class<E> extensionType, ExtensionCallback<R, E, X> callback);

    /**
     * A convenience method which opens an extension of the given type, and yields it to a callback. A handle is opened
     * if needed by the extention, and closed before returning to the caller.
     *
     * <p>
     * The callback will be executed in a separate thread
     * </p>
     *
     * @param extensionType the type of extension
     * @param callback      a callback which will receive the extension
     * @param <E>           the extension type
     * @param <X>           the exception type optionally thrown by the callback
     * @return a completion stage which completes when the callback returns
     * or throws an exception, or will complete with NoSuchExtensionException if no
     * {@link ExtensionFactory} is registered which supports the given extension type.
     */
    <E, X extends Exception> CompletionStage<Void> useExtension(Class<E> extensionType, ExtensionConsumer<E, X> callback);
}

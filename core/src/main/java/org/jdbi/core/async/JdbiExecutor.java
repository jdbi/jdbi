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
package org.jdbi.core.async;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import org.jdbi.core.HandleCallback;
import org.jdbi.core.HandleConsumer;
import org.jdbi.core.Jdbi;
import org.jdbi.core.extension.ExtensionCallback;
import org.jdbi.core.extension.ExtensionConsumer;
import org.jdbi.core.extension.ExtensionFactory;
import org.jdbi.core.transaction.TransactionIsolationLevel;
import org.jdbi.meta.Beta;

@Beta
public interface JdbiExecutor {

    /**
     * Create a {@link JdbiExecutor}.
     *
     * <p>
     * The executor to pass in needs to be sized to the use case. A rule of thumb is to have the max number of workers be equal to the max number of connections
     * in the connection pool. The worker queue in the executor should probably be bounded, unless the caller(s) already has a bound for the number of
     * outstanding requests. Making the queue bounded will mean you are blocking the calling thread when the queue fills up. Whether or not that is acceptable
     * depends on if and how your service limits incoming requests
     * </p>
     *
     * @param jdbi     the initialized Jdbi class
     * @param executor an executor to use for all database calls
     */
    static JdbiExecutor create(Jdbi jdbi, Executor executor) {
        return new JdbiExecutorImpl(jdbi, executor);
    }

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback for use by clients.
     *
     * <p>
     * The callback will be executed in a thread supplied by the executor
     * </p>
     *
     * @param callback A callback which will receive an open Handle
     * @param <R>      type returned by the callback
     * @param <X>      exception type thrown by the callback, if any.
     * @return a completion stage which completes when the callback returns a value or throws an exception
     */
    <R, X extends Exception> CompletionStage<R> withHandle(HandleCallback<R, X> callback);

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback for use by clients. The handle will be in a transaction when
     * the callback is invoked, and that transaction will be committed if the callback finishes normally, or rolled back if the callback raises an exception.
     *
     * <p>
     * The callback will be executed in a thread supplied by the executor
     * </p>
     *
     * @param callback A callback which will receive an open Handle, in a transaction
     * @param <R>      type returned by the callback
     * @param <X>      exception type thrown by the callback, if any.
     * @return a completion stage which completes when the callback returns a value or throws an exception
     */
    <R, X extends Exception> CompletionStage<R> inTransaction(HandleCallback<R, X> callback);

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback for use by clients. The handle will be in a transaction when
     * the callback is invoked, and that transaction will be committed if the callback finishes normally, or rolled back if the callback raises an exception.
     *
     * <p>
     * This form accepts a transaction isolation level which will be applied to the connection for the scope of this transaction, after which the original
     * isolation level will be restored.
     * </p>
     *
     * <p>
     * The callback will be executed in a thread supplied by the executor
     * </p>
     *
     * @param level    the transaction isolation level which will be applied to the connection for the scope of this transaction, after which the original
     *                 isolation level will be restored.
     * @param callback A callback which will receive an open Handle, in a transaction
     * @param <R>      type returned by the callback
     * @param <X>      exception type thrown by the callback, if any.
     * @return a completion stage which completes when the callback returns a value or throws an exception
     */
    <R, X extends Exception> CompletionStage<R> inTransaction(TransactionIsolationLevel level, HandleCallback<R, X> callback);

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback for use by clients.
     *
     * <p>
     * The callback will be executed in a thread supplied by the executor
     * </p>
     *
     * @param consumer A callback which will receive an open Handle
     * @param <X>      exception type thrown by the callback, if any.
     * @return a completion stage which completes when the callback returns or throws an exception
     */
    <X extends Exception> CompletionStage<Void> useHandle(HandleConsumer<X> consumer);

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback for use by clients. The handle will be in a transaction when
     * the callback is invoked, and that transaction will be committed if the callback finishes normally, or rolled back if the callback raises an exception.
     *
     * <p>
     * The callback will be executed in a thread supplied by the executor
     * </p>
     *
     * @param callback A callback which will receive an open Handle, in a transaction
     * @param <X>      exception type thrown by the callback, if any.
     * @return a completion stage which completes when the callback returns or throws an exception
     */
    <X extends Exception> CompletionStage<Void> useTransaction(HandleConsumer<X> callback);

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback for use by clients. The handle will be in a transaction when
     * the callback is invoked, and that transaction will be committed if the callback finishes normally, or rolled back if the callback raises an exception.
     *
     * <p>
     * This form accepts a transaction isolation level which will be applied to the connection for the scope of this transaction, after which the original
     * isolation level will be restored.
     * </p>
     *
     * <p>
     * The callback will be executed in a thread supplied by the executor
     * </p>
     *
     * @param level    the transaction isolation level which will be applied to the connection for the scope of this transaction, after which the original
     *                 isolation level will be restored.
     * @param callback A callback which will receive an open Handle, in a transaction
     * @param <X>      exception type thrown by the callback, if any.
     * @return a completion stage which completes when the callback returns or throws an exception
     */
    <X extends Exception> CompletionStage<Void> useTransaction(TransactionIsolationLevel level, HandleConsumer<X> callback);

    /**
     * A convenience method which opens an extension of the given type, yields it to a callback, and returns the result of the callback. A handle is opened if
     * needed by the extension, and closed before returning to the caller.
     *
     * <p>
     * The callback will be executed in a thread supplied by the executor
     * </p>
     *
     * @param extensionType the type of extension.
     * @param callback      a callback which will receive the extension.
     * @param <R>           the return type
     * @param <E>           the extension type
     * @param <X>           the exception type optionally thrown by the callback
     * @return a completion stage which completes when the callback returns a value or throws an exception, or will complete with NoSuchExtensionException if no
     * {@link ExtensionFactory} is registered which supports the given extension type.
     */
    <R, E, X extends Exception> CompletionStage<R> withExtension(Class<E> extensionType, ExtensionCallback<R, E, X> callback);

    /**
     * A convenience method which opens an extension of the given type, and yields it to a callback. A handle is opened if needed by the extention, and closed
     * before returning to the caller.
     *
     * <p>
     * The callback will be executed in a thread supplied by the executor
     * </p>
     *
     * @param extensionType the type of extension
     * @param callback      a callback which will receive the extension
     * @param <E>           the extension type
     * @param <X>           the exception type optionally thrown by the callback
     * @return a completion stage which completes when the callback returns or throws an exception, or will complete with NoSuchExtensionException if no
     * {@link ExtensionFactory} is registered which supports the given extension type.
     */
    <E, X extends Exception> CompletionStage<Void> useExtension(Class<E> extensionType, ExtensionConsumer<E, X> callback);
}

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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.extension.ExtensionCallback;
import org.jdbi.v3.core.extension.ExtensionConsumer;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

public class JdbiExecutorImpl implements JdbiExecutor {

    private final Jdbi jdbi;
    private final Executor executor;

    /**
     * Construct a {@link JdbiExecutor}.
     *
     * <p>
     * The executor to pass in needs to be sized to the use case. A rule of thumb is to
     * have the max number of workers be equal to the max number of connections in the
     * connection pool. The worker queue in the executor should probably be bounded, unless
     * the caller(s) already has a bound for the number of outstanding requests. Making the
     * queue bounded will mean you are blocking the calling thread when the queue fills up.
     * Whether or not that is a
     * </p>
     *
     * @param jdbi the initialized Jdbi class
     * @param executor an executor to use for all database calls
     */
    public JdbiExecutorImpl(Jdbi jdbi, Executor executor) {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    /**
     * Single method through which all other methods converge. If you want to override JdbiExecutorImpl, you only need to override this one method.
     *
     * @param handler the handler that takes a Jdbi instance and returns a value
     * @param <R>     type returned by the callback
     * @param <X>     exception type thrown by the callback, if any.
     * @return a completion stage that will complete when the handler returns or throws an exception
     */
    protected <R, X extends Exception> CompletionStage<R> doExecute(final Handler<R, X> handler) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return handler.apply(jdbi);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public <R, X extends Exception> CompletionStage<R> withHandle(final HandleCallback<R, X> callback) {
        return doExecute(jdbi -> jdbi.withHandle(callback));
    }

    @Override
    public <R, X extends Exception> CompletionStage<R> inTransaction(final HandleCallback<R, X> callback) {
        return doExecute(jdbi -> jdbi.inTransaction(callback));
    }

    @Override
    public <R, X extends Exception> CompletionStage<R> inTransaction(final TransactionIsolationLevel level, final HandleCallback<R, X> callback) {
        return doExecute(jdbi -> jdbi.inTransaction(level, callback));
    }

    @Override
    public <X extends Exception> CompletionStage<Void> useHandle(final HandleConsumer<X> consumer) {
        return doExecute(jdbi -> {
            jdbi.useHandle(consumer);
            return null;
        });
    }

    @Override
    public <X extends Exception> CompletionStage<Void> useTransaction(final HandleConsumer<X> callback) {
        return doExecute(jdbi -> {
            jdbi.useTransaction(callback);
            return null;
        });
    }

    @Override
    public <X extends Exception> CompletionStage<Void> useTransaction(final TransactionIsolationLevel level, final HandleConsumer<X> callback) {
        return doExecute(jdbi -> {
            jdbi.useTransaction(level, callback);
            return null;
        });
    }

    @Override
    public <R, E, X extends Exception> CompletionStage<R> withExtension(final Class<E> extensionType, final ExtensionCallback<R, E, X> callback) {
        return doExecute(jdbi -> jdbi.withExtension(extensionType, callback));
    }

    @Override
    public <E, X extends Exception> CompletionStage<Void> useExtension(final Class<E> extensionType, final ExtensionConsumer<E, X> callback) {
        return doExecute(jdbi -> {
            jdbi.useExtension(extensionType, callback);
            return null;
        });
    }

    protected interface Handler<R, X extends Exception> {

        R apply(Jdbi jdbi) throws X;
    }
}

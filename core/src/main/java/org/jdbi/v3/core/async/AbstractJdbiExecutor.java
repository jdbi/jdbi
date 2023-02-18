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
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.extension.ExtensionCallback;
import org.jdbi.v3.core.extension.ExtensionConsumer;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

public abstract class AbstractJdbiExecutor implements JdbiExecutor {

    /**
     * Single method through which all other methods converge. If you want to override JdbiExecutorImpl, you only need to override this one method.
     *
     * @param handler the handler that takes a Jdbi instance and returns a value
     * @param <R>     type returned by the callback
     * @param <X>     exception type thrown by the callback, if any.
     * @return a completion stage that will complete when the handler returns or throws an exception
     */
    protected abstract <R, X extends Exception> CompletionStage<R> doExecute(Handler<R, X> handler);

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

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
import org.jdbi.v3.core.internal.exceptions.CheckedConsumer;
import org.jdbi.v3.core.internal.exceptions.CheckedFunction;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

public abstract class AbstractJdbiExecutor implements JdbiExecutor {

    /**
     * Single method through which all other with* methods converge. Since useExecute also calls this, any implementation of AbstractJdbiExecutor only needs to
     * implement this method
     *
     * @param callback the callback that takes a Jdbi instance and returns a value
     * @param <T>      type returned by the callback
     * @return a completion stage that will complete when the handler returns a value or throws an exception
     */
    protected abstract <T> CompletionStage<T> withExecute(CheckedFunction<Jdbi, T> callback);

    /**
     * Single method through which all other use* methods converge. This method calls {@link #withExecute(CheckedFunction)}
     *
     * @param callback the callback that takes a Jdbi instance
     * @return a completion stage that will complete when the handler returns or throws an exception
     */
    protected CompletionStage<Void> useExecute(CheckedConsumer<Jdbi> callback) {
        return withExecute(jdbi -> {
            callback.accept(jdbi);
            return null;
        });
    }

    @Override
    public <R, X extends Exception> CompletionStage<R> withHandle(final HandleCallback<R, X> callback) {
        return withExecute(jdbi -> jdbi.withHandle(callback));
    }

    @Override
    public <R, X extends Exception> CompletionStage<R> inTransaction(final HandleCallback<R, X> callback) {
        return withExecute(jdbi -> jdbi.inTransaction(callback));
    }

    @Override
    public <R, X extends Exception> CompletionStage<R> inTransaction(final TransactionIsolationLevel level, final HandleCallback<R, X> callback) {
        return withExecute(jdbi -> jdbi.inTransaction(level, callback));
    }

    @Override
    public <X extends Exception> CompletionStage<Void> useHandle(final HandleConsumer<X> consumer) {
        return useExecute(jdbi -> jdbi.useHandle(consumer));
    }

    @Override
    public <X extends Exception> CompletionStage<Void> useTransaction(final HandleConsumer<X> callback) {
        return useExecute(jdbi -> jdbi.useTransaction(callback));
    }

    @Override
    public <X extends Exception> CompletionStage<Void> useTransaction(final TransactionIsolationLevel level, final HandleConsumer<X> callback) {
        return useExecute(jdbi -> jdbi.useTransaction(level, callback));
    }

    @Override
    public <R, E, X extends Exception> CompletionStage<R> withExtension(final Class<E> extensionType, final ExtensionCallback<R, E, X> callback) {
        return withExecute(jdbi -> jdbi.withExtension(extensionType, callback));
    }

    @Override
    public <E, X extends Exception> CompletionStage<Void> useExtension(final Class<E> extensionType, final ExtensionConsumer<E, X> callback) {
        return useExecute(jdbi -> jdbi.useExtension(extensionType, callback));
    }
}

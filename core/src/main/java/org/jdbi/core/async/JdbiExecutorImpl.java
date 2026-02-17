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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import org.jdbi.core.Jdbi;
import org.jdbi.core.internal.exceptions.CheckedFunction;
import org.jdbi.meta.Beta;

@Beta
class JdbiExecutorImpl extends AbstractJdbiExecutor {

    private final Jdbi jdbi;
    private final Executor executor;

    /**
     * Construct a {@link JdbiExecutor}.
     *
     * @param jdbi     the initialized Jdbi class
     * @param executor an executor to use for all database calls
     */
    JdbiExecutorImpl(Jdbi jdbi, Executor executor) {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    /**
     * Make sure to run the callback in a thread supplied by the executor
     *
     * @param callback the callback that takes a Jdbi instance and returns a value
     * @param <T>      type returned by the callback
     * @return a completion stage that will complete when the callback returns a value or throws an exception
     */
    @Override
    protected <T> CompletionStage<T> withExecute(final CheckedFunction<Jdbi, T> callback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callback.apply(jdbi);
            } catch (Throwable t) {
                throw new CompletionException(t);
            }
        }, executor);
    }
}

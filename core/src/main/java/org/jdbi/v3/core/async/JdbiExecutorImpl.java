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

import org.jdbi.v3.core.Jdbi;

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
     * Make sure to run the handler in a thread supplied by the executor
     *
     * @param handler the handler that takes a Jdbi instance and returns a value
     * @param <R>     type returned by the callback
     * @param <X>     exception type thrown by the callback, if any.
     * @return a completion stage that will complete when the handler returns or throws an exception
     */
    @Override
    protected <R, X extends Exception> CompletionStage<R> doExecute(final Handler<R, X> handler) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return handler.apply(jdbi);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
}

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
package org.jdbi.v3.core.statement;

import java.time.Instant;

/**
 * SqlLoggers receive query data before and after a query is executed, and after an exception is thrown by a bad query.
 *
 * Defined attributes (see {@link SqlStatement#define(String, Object)}) and bound {@link org.jdbi.v3.core.argument.Argument}s (see {@link SqlStatement#bind(String, Object)}) are available on the {@link StatementContext}, along with timing information using {@link java.time.Instant}s. It's recommendable to use {@link java.time.temporal.ChronoUnit#between} to measure elapsed time in your unit of choice, as in {@link StatementContext#getElapsedTime}.
 *
 * See also {@link org.jdbi.v3.core.argument.LoggableArgument}. They can enable you to log previously unloggable {@link org.jdbi.v3.core.argument.Argument}s by wrapping them with a value in {@link org.jdbi.v3.core.argument.ArgumentFactory}s.
 */
public interface SqlLogger {
    SqlLogger NOP_SQL_LOGGER = new SqlLogger() {
    };

    default void logBeforeExecution(StatementContext context) {
    }

    default void logAfterExecution(StatementContext context) {
    }

    default <X extends Exception> void logException(StatementContext context, X ex) {
    }

    default <T, X extends Exception> T wrap(SqlLoggable<T, X> r, StatementContext ctx) throws X {
        try {
            ctx.setExecutionMoment(Instant.now());
            logBeforeExecution(ctx);

            T result = r.invoke();

            ctx.setCompletionMoment(Instant.now());
            logAfterExecution(ctx);

            return result;
        } catch (Exception e) {
            ctx.setExceptionMoment(Instant.now());
            logException(ctx, (X) e);
            throw (X) e;
        }
    }
}

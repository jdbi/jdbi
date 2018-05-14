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

import java.sql.SQLException;
import java.time.Instant;

/**
 * SqlLoggers receive query data before and after a query is executed, and after an exception is thrown by a bad query.
 *
 * Defined attributes (see {@link SqlStatement#define(String, Object)}) and bound {@link org.jdbi.v3.core.argument.Argument}s (see {@link SqlStatement#bind(String, Object)}) are available on the {@link StatementContext}, along with timing information using {@link java.time.Instant}s. It's recommendable to use {@link java.time.temporal.ChronoUnit#between} to measure elapsed time in your unit of choice, as in {@link StatementContext#getElapsedTime}.
 *
 * Note that if you {@code bind} an {@link org.jdbi.v3.core.argument.Argument} instance directly, it must implement {@link Object#toString} if you want to be able to log it in any meaningful way. You can also implement log censorship that way, e.g. to hide sensitive content like passwords.
 */
public interface SqlLogger {
    SqlLogger NOP_SQL_LOGGER = new SqlLogger() {
    };

    default void logBeforeExecution(StatementContext context) {
    }

    default void logAfterExecution(StatementContext context) {
    }

    default void logException(StatementContext context, SQLException ex) {
    }

    default <T> T wrap(SqlLoggable<T> r, StatementContext ctx) throws SQLException {
        try {
            ctx.setExecutionMoment(Instant.now());
            logBeforeExecution(ctx);

            T result = r.invoke();

            ctx.setCompletionMoment(Instant.now());
            logAfterExecution(ctx);

            return result;
        } catch (SQLException e) {
            ctx.setExceptionMoment(Instant.now());
            logException(ctx, e);
            throw e;
        }
    }
}

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

import org.jdbi.v3.core.internal.UtilityClassException;

class SqlLoggerUtil {
    private SqlLoggerUtil() {
        throw new UtilityClassException();
    }

    static <T> T wrap(SqlLoggable<T> r, StatementContext ctx, SqlLogger logger) throws SQLException {
        try {
            ctx.setExecutionMoment(Instant.now());
            logger.logBeforeExecution(ctx);

            T result = r.invoke();

            ctx.setCompletionMoment(Instant.now());
            logger.logAfterExecution(ctx);

            return result;
        } catch (SQLException e) {
            ctx.setExceptionMoment(Instant.now());
            logger.logException(ctx, e);
            throw e;
        }
    }
}

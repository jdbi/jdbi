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

/**
 * SqlLoggers receive query data before and after a query is executed, and after an exception is thrown by a bad query. The elapsed time in the former case, and the exception in the latter case, are passed as well. Defined attributes (see {@link SqlStatement#define(String, Object)}) and bound {@link org.jdbi.v3.core.argument.Argument}s (see {@link SqlStatement#bind(String, Object)}) are also available.
 *
 * See also {@link org.jdbi.v3.core.argument.LoggableArgument}. They can enable you to log previously unloggable {@link org.jdbi.v3.core.argument.Argument}s by wrapping them with a value in {@link org.jdbi.v3.core.argument.ArgumentFactory}s.
 */
public interface SqlLogger {
    SqlLogger NOP_SQL_LOGGER = new SqlLogger() {
        @Override
        public void logBeforeExecution(StatementContext context) {}

        @Override
        public void logAfterExecution(StatementContext context, long nanos) {}

        @Override
        public void logException(StatementContext context, SQLException ex) {}
    };

    void logBeforeExecution(StatementContext context);

    void logAfterExecution(StatementContext context, long nanos);

    void logException(StatementContext context, SQLException ex);
}

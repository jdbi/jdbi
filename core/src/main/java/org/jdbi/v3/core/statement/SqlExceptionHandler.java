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

import org.jdbi.v3.meta.Alpha;

/**
 * Handler for exceptions thrown while executing SQL statements.
 */
@Alpha
@FunctionalInterface
public interface SqlExceptionHandler {
    /**
     * Take action based on a SQLException thrown during statement execution.
     * If you would like to replace the thrown exception with a new one, you
     * may {@code throw} from this method. If the method returns normally,
     * exception handling proceeds to the next oldest registered handler.
     * If no handler opts to {@code throw}, the original exception will propagate
     * wrapped by an {@link UnableToExecuteStatementException}.
     * @param ex the exception thrown
     * @param ctx the statement context
     */
    void handle(SQLException ex, StatementContext ctx);
}

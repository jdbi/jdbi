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
package org.jdbi.v3.core.argument;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;

/**
 * Represents an argument to a prepared statement. It will be called right before the
 * statement is executed to bind the parameter.
 *
 * Make sure to override {@link Object#toString} if you want to be able to log values with an {@link org.jdbi.v3.core.statement.SqlLogger}.
 */
@FunctionalInterface
public interface Argument {
    /**
     * Apply the argument to the given prepared statement.
     *
     * @param position the position to which the argument should be bound, using the
     *                 stupid JDBC "start at 1" bit
     * @param statement the prepared statement the argument is to be bound to
     * @param ctx the statement context
     * @throws SQLException if anything goes wrong
     */
    void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException;
}

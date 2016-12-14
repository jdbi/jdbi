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
package org.jdbi.v3.core;

import static org.jdbi.v3.core.Cleanables.forResultSet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jdbi.v3.core.exception.ResultSetException;

/**
 * Commonly used ResultProducer implementations.
 */
public class ResultProducers {
    /**
     * Result producer that eagerly executes the statement, returning the update count
     *
     * @return update count
     * @see PreparedStatement#getUpdateCount()
     */
    public static ResultProducer<Integer> returningUpdateCount() {
        return (statementSupplier, ctx) -> {
            try {
                return statementSupplier.get().getUpdateCount();
            } finally {
                ctx.close();
            }
        };
    }

    /**
     * Result producer that returns a {@link ResultSetIterable} over the statement result rows.
     *
     * @return ResultSetIterable of result rows.
     * @see PreparedStatement#getResultSet()
     */
    public static ResultProducer<ResultSetIterable> returningResults() {
        return (supplier, ctx) -> ResultSetIterable.of(getResultSet(supplier, ctx), ctx);
    }

    private static Supplier<ResultSet> getResultSet(Supplier<PreparedStatement> supplier, StatementContext ctx) {
        return () -> {
            try {
                ResultSet rs = supplier.get().getResultSet();
                if (rs != null) {
                    ctx.addCleanable(forResultSet(rs));
                }
                return rs;
            } catch (SQLException e) {
                throw new ResultSetException("Could not get result set", e, ctx);
            }
        };
    }

    /**
     * Result producer that returns a {@link ResultSetIterable} over the statement-generated keys.
     *
     * @param generatedKeyColumnNames optional list of generated key column names.
     * @return ResultSetIterable of generated keys
     * @see PreparedStatement#getGeneratedKeys()
     */
    public static ResultProducer<ResultSetIterable> returningGeneratedKeys(String... generatedKeyColumnNames) {
        return (supplier, ctx) -> {
            String[] columnNames = Stream.of(generatedKeyColumnNames)
                    .filter(name -> name != null && !name.isEmpty())
                    .toArray(String[]::new);

            ctx.setReturningGeneratedKeys(true);

            if (columnNames.length > 0) {
                ctx.setGeneratedKeysColumnNames(columnNames);
            }

            return ResultSetIterable.of(getGeneratedKeys(supplier, ctx), ctx);
        };
    }

    private static Supplier<ResultSet> getGeneratedKeys(Supplier<PreparedStatement> supplier, StatementContext ctx) {
        return () -> {
            try {
                ResultSet rs = supplier.get().getGeneratedKeys();
                if (rs != null) {
                    ctx.addCleanable(forResultSet(rs));
                }
                return rs;
            } catch (SQLException e) {
                throw new ResultSetException("Could not get generated keys", e, ctx);
            }
        };
    }

}

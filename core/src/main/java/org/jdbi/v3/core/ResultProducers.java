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
import java.util.stream.Stream;

import org.jdbi.v3.core.exception.ResultSetException;

public class ResultProducers {
    public static ResultProducer<Integer> returningUpdateCount() {
        return (stmt, ctx) -> stmt.getUpdateCount();
    }

    public static ResultProducer<ResultSetIterable> returningResults() {
        return (stmt, ctx) -> ResultSetIterable.of(getResultSet(stmt, ctx), ctx);
    }

    public static ResultProducer<ResultSetIterable> returningGeneratedKeys(String... generatedKeyColumnNames) {
        return new ResultProducer<ResultSetIterable>() {
            @Override
            public void beforeExecute(BaseStatement<?> stmt) {
                String[] columnNames = Stream.of(generatedKeyColumnNames)
                        .filter(name -> name != null && !name.isEmpty())
                        .toArray(String[]::new);

                stmt.getContext().setReturningGeneratedKeys(true);
                if (columnNames.length > 0) {
                    stmt.getContext().setGeneratedKeysColumnNames(columnNames);
                }
            }

            @Override
            public ResultSetIterable produce(PreparedStatement stmt, StatementContext ctx) throws SQLException {
                return ResultSetIterable.of(getGeneratedKeys(stmt, ctx), ctx);
            }
        };
    }

    private static ResultSet getGeneratedKeys(PreparedStatement stmt, StatementContext ctx) {
        ResultSet rs;
        try {
            rs = stmt.getGeneratedKeys();
        } catch (SQLException e) {
            throw new ResultSetException("Could not get generated keys", e, ctx);
        }
        if (rs != null) {
            ctx.addCleanable(forResultSet(rs));
        }
        return rs;
    }

    private static ResultSet getResultSet(PreparedStatement stmt, StatementContext ctx) {
        ResultSet rs;
        try {
            rs = stmt.getResultSet();
        } catch (SQLException e) {
            throw new ResultSetException("Could not get result set", e, ctx);
        }
        if (rs != null) {
            ctx.addCleanable(forResultSet(rs));
        }
        return rs;
    }

}

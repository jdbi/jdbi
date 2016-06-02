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
package org.jdbi.v3;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jdbi.v3.statement.StatementBuilder;
import org.jdbi.v3.transaction.TransactionState;

/**
 * Resource management for JDBI. Cleanables can be registered on a SQL statement and they get cleaned up when the
 * statement finishes or (in the case of a ResultIterator), the object representing the results is closed.
 *
 * Resources managed by JDBI are {@link ResultSet}, {@link Statement}, {@link Handle} and {@link StatementBuilder} for historical reasons.
 */
class Cleanables
{
    @FunctionalInterface
    interface Cleanable extends AutoCloseable
    {
        @Override
        void close() throws SQLException;
    }

    private Cleanables()
    {
        throw new AssertionError("do not instantiate");
    }

    static Cleanable forResultSet(final ResultSet rs)
    {
        return rs::close;
    }

    static Cleanable forStatement(final Statement stmt)
    {
        return stmt::close;
    }

    static Cleanable forHandle(final Handle handle, final TransactionState state)
    {
        return () -> {
            if (handle != null) {
                if (handle.isInTransaction()) {
                    if (state == TransactionState.COMMIT) {
                        handle.commit();
                    }
                    else {
                        handle.rollback();
                    }
                }
                handle.close();
            }
        };
    }

    static Cleanable forStatementBuilder(StatementBuilder statementBuilder, Connection conn, String sql, PreparedStatement stmt) {
        return () -> statementBuilder.close(conn, sql, stmt);
    }
}

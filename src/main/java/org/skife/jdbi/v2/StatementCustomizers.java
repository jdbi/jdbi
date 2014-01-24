/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.BaseStatementCustomizer;
import org.skife.jdbi.v2.tweak.StatementCustomizer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class StatementCustomizers
{
    private StatementCustomizers()
    {
    }

    /**
     * Hint to the statement, that we want only a single row. Used by {@link Query#first()} to limit the number
     * of rows returned by the database.
     */
    public static final StatementCustomizer MAX_ROW_ONE = new MaxRowsCustomizer(1);

    /**
     * Sets the fetch direction on a query. Can be used as a Statement customizer or a SqlStatementCustomizer.
     */
    public static class FetchDirectionStatementCustomizer extends BaseStatementCustomizer
    {
        private final Integer direction;

        public FetchDirectionStatementCustomizer(final Integer direction)
        {
            this.direction = direction;
        }

        @Override
        public void beforeExecution(final PreparedStatement stmt, final StatementContext ctx) throws SQLException
        {
            stmt.setFetchDirection(direction);
        }

        public void apply(SQLStatement q) throws SQLException
        {
            q.setFetchDirection(direction);
        }
    }

    public static final class QueryTimeoutCustomizer extends BaseStatementCustomizer
    {
        private final int seconds;

        public QueryTimeoutCustomizer(final int seconds)
        {
            this.seconds = seconds;
        }

        @Override
        public void beforeExecution(final PreparedStatement stmt, final StatementContext ctx) throws SQLException
        {
            stmt.setQueryTimeout(seconds);
        }
    }

    public static final class FetchSizeCustomizer extends BaseStatementCustomizer
    {
        private final int fetchSize;

        public FetchSizeCustomizer(final int fetchSize)
        {
            this.fetchSize = fetchSize;
        }

        @Override
        public void beforeExecution(final PreparedStatement stmt, final StatementContext ctx) throws SQLException
        {
            stmt.setFetchSize(fetchSize);
        }
    }

    public static final class MaxRowsCustomizer extends BaseStatementCustomizer
    {
        private final int maxRows;

        public MaxRowsCustomizer(final int maxRows)
        {
            this.maxRows = maxRows;
        }

        @Override
        public void beforeExecution(final PreparedStatement stmt, final StatementContext ctx) throws SQLException
        {
            stmt.setMaxRows(maxRows);
        }
    }

    public static final class MaxFieldSizeCustomizer extends BaseStatementCustomizer
    {
        private final int maxFieldSize;

        public MaxFieldSizeCustomizer(final int maxFieldSize)
        {
            this.maxFieldSize = maxFieldSize;
        }

        @Override
        public void beforeExecution(final PreparedStatement stmt, final StatementContext ctx) throws SQLException
        {
            stmt.setMaxFieldSize(maxFieldSize);
        }
    }
}

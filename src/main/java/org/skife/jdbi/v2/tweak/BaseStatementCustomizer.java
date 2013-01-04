/*
 * Copyright 2004 - 2011 Brian McCallister
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

package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.StatementContext;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Convenience class which provides no-op stubs of the StatementCustomizer methods
 */
public class BaseStatementCustomizer implements StatementCustomizer
{
    /**
     * Make the changes you need to inside this method. It will be invoked prior to execution of
     * the prepared statement
     *
     * @param stmt Prepared statement being customized
     * @param ctx  Statement context associated with the statement being customized
     *
     * @throws java.sql.SQLException go ahead and percolate it for jDBI to handle
     */
    public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
    {
    }

    /**
     * This will be invoked after execution of the prepared statement, but before any results
     * are accessed.
     *
     * @param stmt Prepared statement being customized
     * @param ctx  Statement context associated with the statement being customized
     *
     * @throws java.sql.SQLException go ahead and percolate it for jDBI to handle
     */
    public void afterExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
    {
    }


    /**
     * Invoked at cleanup time to clean resources used by this statement.
     *
     * @param ctx Statement context associated with the statement being customized
     * @throws SQLException go ahead and percolate it for jDBI to handle
     */
    public void cleanup(final StatementContext ctx) throws SQLException
    {
    }
}

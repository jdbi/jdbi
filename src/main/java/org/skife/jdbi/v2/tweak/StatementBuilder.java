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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Used to convert translated SQL into a prepared statement. The default implementation
 * created by {@link org.skife.jdbi.v2.CachingStatementBuilderFactory} caches all prepared
 * statements created against a given handle.
 *
 * A StatementBuilder is always associated with exactly one Handle instance
 *
 * @see StatementBuilderFactory
 */
public interface StatementBuilder
{
    /**
     * Called each time a prepared statement needs to be created
     *
     * @param conn the JDBC Connection the statement is being created for
     * @param sql the translated SQL which should be prepared
     * @param ctx Statement context associated with the SQLStatement this is building for
     */
    PreparedStatement create(Connection conn, String sql, StatementContext ctx) throws SQLException;

	/**
	 * Called each time a Callable statement needs to be created
	 *
	 * @param conn the JDBC Connection the statement is being created for
	 * @param sql the translated SQL which should be prepared
	 * @param ctx Statement context associated with the SQLStatement this is building for
	 */
	CallableStatement createCall(Connection conn, String sql, StatementContext ctx) throws SQLException;


    /**
     * Called to close an individual prepared statement created from this builder
     *
     * @param sql the translated SQL which was prepared
     * @param stmt the statement
     *
     * @throws SQLException if anything goes wrong closing the statement
     */
    void close(Connection conn, String sql, Statement stmt) throws SQLException;

    /**
     * Called when the handle this StatementBuilder is attached to is closed.
     */
    void close(Connection conn);
}

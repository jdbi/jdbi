/* Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * Used for INSERT, UPDATE, and DELETE statements
 */
public class Update extends SQLStatement<Update>
{
    Update(Connection connection, StatementLocator locator, StatementRewriter statementRewriter, StatementBuilder cache, String sql)
    {
        super(new Binding(), locator, statementRewriter, connection, cache, sql);
    }

    /**
     * For databases which support returning result sets from DML, this method
     * provides a convenient way to get to those results.
     *
     * @return This update statement returning a result set
     */
    public Query<Map<String, Object>> returning() {
        return new Query<Map<String, Object>>(this.getParameters(),
                                              new DefaultMapper(),
                                              this.getStatementLocator(),
                                              this.getRewriter(),
                                              this.getConnection(),
                                              this.getPreparedStatementCache(),
                                              this.getSql());
    }

    /**
     * For databases which support returning result sets from DML, this method
     * provides a convenient way to get to those results. This form provides
     * for mapping attributes onto a JavaBean by name
     *
     * @param type The class of the JavaBean to be instantiated and have
     *             results mapped onto
     * @return This update statement returning a result set
     */
    public <ResultType> Query<ResultType> returning(Class<ResultType> type) {
        return new Query<ResultType>(this.getParameters(),
                                              new BeanMapper<ResultType>(type),
                                              this.getStatementLocator(),
                                              this.getRewriter(),
                                              this.getConnection(),
                                              this.getPreparedStatementCache(),
                                              this.getSql());
    }

    /**
     * For databases which support returning result sets from DML, this method
     * provides a convenient way to get to those results.
     *
     * @return This update statement returning a result set
     */
    public <ResultType> Query<ResultType> returning(ResultSetMapper<ResultType> type) {
        return new Query<ResultType>(this.getParameters(),
                                              type,
                                              this.getStatementLocator(),
                                              this.getRewriter(),
                                              this.getConnection(),
                                              this.getPreparedStatementCache(),
                                              this.getSql());
    }

    /**
     * Execute the statement
     * @return the number of rows modified
     */
    public int execute()
    {
        return this.internalExecute(QueryPreperator.NO_OP, new QueryResultMunger<Integer>()
        {
            public Pair<Integer, ResultSet> munge(Statement results) throws SQLException
            {
                return new Pair<Integer, ResultSet>(results.getUpdateCount(), null);
            }
        }, QueryPostMungeCleanup.CLOSE_RESOURCES_QUIETLY);
    }
}

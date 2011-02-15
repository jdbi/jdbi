/*
 * Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;

import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.StatementCustomizer;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;

/**
 * Used for INSERT, UPDATE, and DELETE statements
 */
public class Update extends SQLStatement<Update>
{
    Update(Connection connection,
           StatementLocator locator,
           StatementRewriter statementRewriter,
           StatementBuilder cache,
           String sql,
           StatementContext ctx,
           SQLLog log,
           TimingCollector timingCollector)
    {
        super(new Binding(), locator, statementRewriter, connection, cache, sql, ctx, log, timingCollector, Collections.<StatementCustomizer>emptyList());
    }

    /**
     * Execute the statement
     * @return the number of rows modified
     */
    public int execute()
    {
        return this.internalExecute(QueryPreperator.NO_OP, new QueryResultMunger<Integer>()
        {
            public Integer munge(Statement results) throws SQLException
            {
                return results.getUpdateCount();
            }
        }, QueryPostMungeCleanup.CLOSE_RESOURCES_QUIETLY);
    }

    /**
     * Execute the statement and returns any auto-generated keys. This requires the JDBC driver to support
     * the {@link Statement#getGeneratedKeys()} method.
     * @param the mapper to generate the resulting key object
     * @return the generated key or null if none was returned
     */
    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndReturnGeneratedKeys(final ResultSetMapper<GeneratedKeyType> mapper)
    {
        return this.internalExecute(QueryPreperator.NO_OP, new QueryResultMunger<GeneratedKeys<GeneratedKeyType>>()
        {
            public GeneratedKeys<GeneratedKeyType> munge(Statement results) throws SQLException
            {
                return new GeneratedKeys<GeneratedKeyType>(mapper,
                                                           Update.this,
                                                           results,
                                                           getContext());
            }
        }, QueryPostMungeCleanup.NO_OP);
    }

    public GeneratedKeys<Map<String, Object>> executeAndReturnGeneratedKeys()
    {
        return executeAndReturnGeneratedKeys(new DefaultMapper());
    }
}

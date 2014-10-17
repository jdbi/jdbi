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

import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.StatementCustomizer;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;

/**
 * Used for INSERT, UPDATE, and DELETE statements
 */
public class Update extends SQLStatement<Update>
{
    Update(Handle handle,
           StatementLocator locator,
           StatementRewriter statementRewriter,
           StatementBuilder statementBuilder,
           String sql,
           ConcreteStatementContext ctx,
           SQLLog log,
           TimingCollector timingCollector,
           Foreman foreman,
           ContainerFactoryRegistry containerFactoryRegistry)
    {
        super(new Binding(), locator, statementRewriter, handle, statementBuilder, sql, ctx, log, timingCollector, Collections.<StatementCustomizer>emptyList(), foreman, containerFactoryRegistry);
    }

    /**
     * Execute the statement
     * @return the number of rows modified
     */
    public int execute()
    {
        try {
            return this.internalExecute(new QueryResultMunger<Integer>() {
                public Integer munge(Statement results) throws SQLException
                {
                    return results.getUpdateCount();
                }
            });
        }
        finally {
            cleanup();
        }
    }

    /**
     * Execute the statement and returns any auto-generated keys. This requires the JDBC driver to support
     * the {@link Statement#getGeneratedKeys()} method.
     * @param mapper the mapper to generate the resulting key object
     * @param columnName name of the column that generates the key
     * @return the generated key or null if none was returned
     */
    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndReturnGeneratedKeys(final ResultSetMapper<GeneratedKeyType> mapper, String columnName)
    {
        getConcreteContext().setReturningGeneratedKeys(true);
        if (columnName != null) {
            getConcreteContext().setGeneratedKeysColumnNames(new String[] { columnName } );
        }
        return this.internalExecute(new QueryResultMunger<GeneratedKeys<GeneratedKeyType>>() {
            public GeneratedKeys<GeneratedKeyType> munge(Statement results) throws SQLException
            {
                return new GeneratedKeys<GeneratedKeyType>(mapper,
                                                           Update.this,
                                                           results,
                                                           getContext(),
                                                           getContainerMapperRegistry());
            }
        });
    }

    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndReturnGeneratedKeys(final ResultSetMapper<GeneratedKeyType> mapper) {
        return executeAndReturnGeneratedKeys(mapper, null);
    }

    public GeneratedKeys<Map<String, Object>> executeAndReturnGeneratedKeys()
    {
        return executeAndReturnGeneratedKeys(new DefaultMapper());
    }
}

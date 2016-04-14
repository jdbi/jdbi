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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;

import org.jdbi.v3.exceptions.UnableToExecuteStatementException;
import org.jdbi.v3.tweak.ColumnMapper;
import org.jdbi.v3.tweak.RowMapper;
import org.jdbi.v3.tweak.StatementBuilder;
import org.jdbi.v3.tweak.StatementCustomizer;
import org.jdbi.v3.tweak.StatementLocator;
import org.jdbi.v3.tweak.StatementRewriter;
import org.jdbi.v3.util.SingleColumnMapper;

/**
 * Used for INSERT, UPDATE, and DELETE statements
 */
public class Update extends SQLStatement<Update>
{
    private final MappingRegistry mappingRegistry;

    Update(Handle handle,
           StatementLocator locator,
           StatementRewriter statementRewriter,
           StatementBuilder statementBuilder,
           String sql,
           ConcreteStatementContext ctx,
           TimingCollector timingCollector,
           ArgumentRegistry argumentRegistry,
           MappingRegistry mappingRegistry,
           CollectorFactoryRegistry collectorFactoryRegistry)
    {
        super(new Binding(), locator, statementRewriter, handle, statementBuilder, sql, ctx, timingCollector,
                Collections.<StatementCustomizer>emptyList(), argumentRegistry, collectorFactoryRegistry);
        this.mappingRegistry = mappingRegistry;
    }

    /**
     * Execute the statement
     * @return the number of rows modified
     */
    public int execute()
    {
        try (final PreparedStatement stmt = internalExecute()) {
            return stmt.getUpdateCount();
        } catch (SQLException e) {
            throw new UnableToExecuteStatementException("Could not get update count", e, getContext());
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
    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndReturnGeneratedKeys(final RowMapper<GeneratedKeyType> mapper, String columnName)
    {
        getConcreteContext().setReturningGeneratedKeys(true);
        if (columnName != null && !columnName.isEmpty()) {
            getConcreteContext().setGeneratedKeysColumnNames(new String[] { columnName } );
        }
        return new GeneratedKeys<>(mapper,
                Update.this,
                internalExecute(),
                getContext());
    }

    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndReturnGeneratedKeys(final RowMapper<GeneratedKeyType> mapper) {
        return executeAndReturnGeneratedKeys(mapper, null);
    }

    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndReturnGeneratedKeys(final ColumnMapper<GeneratedKeyType> mapper) {
        return executeAndReturnGeneratedKeys(new SingleColumnMapper<>(mapper), null);
    }

    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndReturnGeneratedKeys(GenericType<GeneratedKeyType> generatedKeyType) {
        return executeAndReturnGeneratedKeys(new RegisteredRowMapper<>(generatedKeyType.getType(), mappingRegistry), null);
    }

    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndReturnGeneratedKeys(Class<GeneratedKeyType> generatedKeyType) {
        return executeAndReturnGeneratedKeys(new RegisteredRowMapper<>(generatedKeyType, mappingRegistry), null);
    }

    public GeneratedKeys<Map<String, Object>> executeAndReturnGeneratedKeys()
    {
        return executeAndReturnGeneratedKeys(new DefaultMapper());
    }
}

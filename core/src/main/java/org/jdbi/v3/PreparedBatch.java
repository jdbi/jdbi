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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jdbi.v3.exception.UnableToCreateStatementException;
import org.jdbi.v3.exception.UnableToExecuteStatementException;
import org.jdbi.v3.mapper.ColumnMapper;
import org.jdbi.v3.mapper.RowMapper;
import org.jdbi.v3.rewriter.RewrittenStatement;
import org.jdbi.v3.statement.StatementBuilder;
import org.jdbi.v3.statement.StatementCustomizer;
import org.jdbi.v3.util.GenericType;
import org.jdbi.v3.util.SingleColumnMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a prepared batch statement. That is, a sql statement compiled as a prepared
 * statement, and then executed multiple times in a single batch. This is, generally,
 * a very efficient way to execute large numbers of the same statement where
 * the statement only varies by the arguments bound to it.
 */
public class PreparedBatch extends SqlStatement<PreparedBatch>
{
    private static final Logger LOG = LoggerFactory.getLogger(PreparedBatch.class);

    private final List<PreparedBatchPart> parts = new ArrayList<>();
    private Binding currentBinding;

    PreparedBatch(JdbiConfig config,
                  Handle handle,
                  StatementBuilder statementBuilder,
                  String sql,
                  StatementContext ctx,
                  Collection<StatementCustomizer> statementCustomizers)
    {
        super(config, new Binding(), handle, statementBuilder, sql, ctx, statementCustomizers);
        this.currentBinding = new Binding();
    }

    /**
     * Specify a value on the statement context for this batch
     *
     * @return self
     */
    @Override
    public PreparedBatch define(String key, Object value)
    {
        getContext().setAttribute(key, value);
        return this;
    }

    /**
     * Adds all key/value pairs in the Map to the {@link StatementContext}.
     *
     * @param values containing key/value pairs.
     * @return this
     */
    @Override
    public PreparedBatch define(final Map<String, ?> values)
    {
        if (values != null) {
            for (Map.Entry<String, ?> entry: values.entrySet())
            {
                getContext().setAttribute(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    /**
     * Execute the batch
     *
     * @return the number of rows modified or inserted per batch part.
     */
    public int[] execute() {
        return (int[]) internalBatchExecute(null, null);
    }

    @SuppressWarnings("unchecked")
    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndGenerateKeys(final RowMapper<GeneratedKeyType> mapper) {
        return (GeneratedKeys<GeneratedKeyType>) internalBatchExecute(results ->
                new GeneratedKeys<>(mapper,
                                    PreparedBatch.this,
                                    results,
                                    getContext()), null);
    }

    @SuppressWarnings("unchecked")
    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndGenerateKeys(final RowMapper<GeneratedKeyType> mapper,
                                                                                     String... columnNames) {
        return (GeneratedKeys<GeneratedKeyType>) internalBatchExecute(results ->
                new GeneratedKeys<>(mapper,
                        PreparedBatch.this,
                        results,
                        getContext()), columnNames);
    }

    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndGenerateKeys(final ColumnMapper<GeneratedKeyType> mapper) {
        return executeAndGenerateKeys(new SingleColumnMapper<>(mapper));
    }

    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndGenerateKeys(GenericType<GeneratedKeyType> generatedKeyType) {
        return executeAndGenerateKeys(rowMapperForType(generatedKeyType));
    }

    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndGenerateKeys(Class<GeneratedKeyType> generatedKeyType) {
        return executeAndGenerateKeys(rowMapperForType(generatedKeyType));
    }

    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndGenerateKeys(Class<GeneratedKeyType> generatedKeyType,
                                                                                     String... columnNames) {
        return executeAndGenerateKeys(rowMapperForType(generatedKeyType), columnNames);
    }

    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndGenerateKeys(ColumnMapper<GeneratedKeyType> mapper,
                                                                                     String columnName) {
        return executeAndGenerateKeys(new SingleColumnMapper<>(mapper, columnName), new String[] { columnName });
    }

    private <Result> Object internalBatchExecute(Function<PreparedStatement, Result> munger, String[] columnNames) {
        boolean generateKeys = munger != null;
        // short circuit empty batch
        if (parts.size() == 0) {
            if (generateKeys) {
                throw new IllegalArgumentException("Unable generate keys for a not prepared batch");
            }
            return new int[]{};
        }

        PreparedBatchPart current = parts.get(0);
        final String rawSql = getSql();
        final RewrittenStatement rewritten = getRewriter().rewrite(rawSql, current.getParams(), getContext());
        PreparedStatement stmt;
        try {
            try {
                Connection connection = getHandle().getConnection();
                if (generateKeys) {
                    if (columnNames != null) {
                        stmt = connection.prepareStatement(rewritten.getSql(), columnNames);
                    } else  {
                        stmt = connection.prepareStatement(rewritten.getSql(), Statement.RETURN_GENERATED_KEYS);
                    }
                } else {
                    stmt = connection.prepareStatement(rewritten.getSql(), Statement.NO_GENERATED_KEYS);
                }
                addCleanable(Cleanables.forStatement(stmt));
            }
            catch (SQLException e) {
                throw new UnableToCreateStatementException(e, getContext());
            }


            try {
                for (PreparedBatchPart part : parts) {
                    rewritten.bind(part.getParams(), stmt);
                    stmt.addBatch();
                }
            }
            catch (SQLException e) {
                throw new UnableToExecuteStatementException("Exception while binding parameters", e, getContext());
            }

            beforeExecution(stmt);

            try {
                final long start = System.nanoTime();
                final int[] rs =  stmt.executeBatch();
                final long elapsedTime = System.nanoTime() - start;
                LOG.trace("Prepared batch of {} parts executed in {}ms", parts.size(), elapsedTime / 1000000L, rewritten.getSql());
                getTimingCollector().collect(elapsedTime, getContext());

                afterExecution(stmt);

                return generateKeys ? munger.apply(stmt) : rs;
            }
            catch (SQLException e) {
                throw new UnableToExecuteStatementException(e, getContext());
            }
        }
        finally {
            try {
                if (!generateKeys) {
                    cleanup();
                }
            }
            finally {
                this.parts.clear();
            }
        }
    }

    /**
     * Add a statement (part) to this batch. You'll need to bindBinaryStream any arguments to the
     * part.
     *
     * @return A part which can be used to bindBinaryStream parts to the statement
     */
    public PreparedBatchPart add()
    {
        PreparedBatchPart part = new PreparedBatchPart(config,
                                                       this.currentBinding,
                                                       this,
                                                       getHandle(),
                                                       getStatementBuilder(),
                                                       getSql(),
                                                       getContext());
        parts.add(part);
        this.currentBinding = new Binding();
        return part;
    }

    public PreparedBatch add(Object... args)
    {
        PreparedBatchPart part = add();
        for (int i = 0; i < args.length; ++i) {
            part.bind(i, args[i]);
        }
        return this;
    }


    /**
     * Create a new batch part by binding values looked up in <code>args</code> to
     * named parameters on the statement.
     *
     * @param args map to bind arguments from for named parameters on the statement
     *
     * @return the new batch part
     */
    public PreparedBatchPart add(Map<String, ?> args)
    {
        PreparedBatchPart part = add();
        part.bindFromMap(args);
        return part;
    }

    /**
     * @return the number of statements which are in this batch
     */
    public int size()
    {
        return parts.size();
    }

    @Override
    protected Binding getParams()
    {
        return this.currentBinding;
    }
}

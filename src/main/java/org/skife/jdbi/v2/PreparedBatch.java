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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.tweak.ResultColumnMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.tweak.RewrittenStatement;
import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.StatementCustomizer;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.util.SingleColumnMapper;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Represents a prepared batch statement. That is, a sql statement compiled as a prepared
 * statement, and then executed multiple times in a single batch. This is, generally,
 * a very efficient way to execute large numbers of the same statement where
 * the statement only varies by the arguments bound to it.
 */
public class PreparedBatch extends SQLStatement<PreparedBatch>
{
    private final List<PreparedBatchPart> parts = new ArrayList<PreparedBatchPart>();
    private Binding currentBinding;

    PreparedBatch(StatementLocator locator,
                  StatementRewriter rewriter,
                  Handle handle,
                  StatementBuilder statementBuilder,
                  String sql,
                  ConcreteStatementContext ctx,
                  SQLLog log,
                  TimingCollector timingCollector,
                  Collection<StatementCustomizer> statementCustomizers,
                  Foreman foreman,
                  ContainerFactoryRegistry containerFactoryRegistry)
    {
        super(new Binding(), locator, rewriter, handle, statementBuilder, sql, ctx, log, timingCollector, statementCustomizers, foreman, containerFactoryRegistry);
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
    public PreparedBatch define(final Map<String, ? extends Object> values)
    {
        if (values != null) {
            for (Map.Entry<String, ? extends Object> entry: values.entrySet())
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
        return (int[]) internalBatchExecute(null);
    }

    @SuppressWarnings("unchecked")
    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndGenerateKeys(final ResultSetMapper<GeneratedKeyType> mapper) {
        return (GeneratedKeys<GeneratedKeyType>) internalBatchExecute(new QueryResultMunger<GeneratedKeys<GeneratedKeyType>>() {
            public GeneratedKeys<GeneratedKeyType> munge(Statement results) throws SQLException {
                return new GeneratedKeys<GeneratedKeyType>(mapper,
                        PreparedBatch.this,
                        results,
                        getContext(),
                        getContainerMapperRegistry());
            }
        });

    }

    @SuppressWarnings("unchecked")
    public <GeneratedKeyType> GeneratedKeys<GeneratedKeyType> executeAndGenerateKeys(final ResultColumnMapper<GeneratedKeyType> mapper) {
        return executeAndGenerateKeys(new SingleColumnMapper<GeneratedKeyType>(mapper));
    }

    private <Result> Object internalBatchExecute(QueryResultMunger<Result> munger) {
        boolean generateKeys = munger != null;
        // short circuit empty batch
        if (parts.size() == 0) {
            if (generateKeys) {
                throw new IllegalArgumentException("Unable generate keys for a not prepared batch");
            }
            return new int[]{};
        }

        PreparedBatchPart current = parts.get(0);
        final String my_sql ;
        try {
            my_sql = getStatementLocator().locate(getSql(), getContext());
        }
        catch (Exception e) {
            throw new UnableToCreateStatementException(String.format("Exception while locating statement for [%s]",
                                                                     getSql()), e, getContext());
        }
        final RewrittenStatement rewritten = getRewriter().rewrite(my_sql, current.getParameters(), getContext());
        PreparedStatement stmt = null;
        try {
            try {
                stmt = getHandle().getConnection().prepareStatement(rewritten.getSql(),
                        generateKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
                addCleanable(Cleanables.forStatement(stmt));
            }
            catch (SQLException e) {
                throw new UnableToCreateStatementException(e, getContext());
            }


            try {
                for (PreparedBatchPart part : parts) {
                    rewritten.bind(part.getParameters(), stmt);
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
                getLog().logPreparedBatch(elapsedTime / 1000000L, rewritten.getSql(), parts.size());
                getTimingCollector().collect(elapsedTime, getContext());

                afterExecution(stmt);

                return generateKeys ? munger.munge(stmt) : rs;
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
        PreparedBatchPart part = new PreparedBatchPart(this.currentBinding,
                                                       this,
                                                       getStatementLocator(),
                                                       getRewriter(),
                                                       getHandle(),
                                                       getStatementBuilder(),
                                                       getSql(),
                                                       getConcreteContext(),
                                                       getLog(),
                                                       getTimingCollector(),
                                                       getForeman(),
                                                       getContainerMapperRegistry());
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
    public PreparedBatchPart add(Map<String, ? extends Object> args)
    {
        PreparedBatchPart part = add();
        part.bindFromMap(args);
        return part;
    }

    /**
     * The number of statements which are in this batch
     */
    public int getSize()
    {
        return parts.size();
    }

    /**
     * The number of statements which are in this batch
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

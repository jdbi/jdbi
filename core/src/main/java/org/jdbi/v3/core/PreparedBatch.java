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
package org.jdbi.v3.core;

import static org.jdbi.v3.core.ResultProducers.returningGeneratedKeys;
import static org.jdbi.v3.core.ResultProducers.returningResults;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jdbi.v3.core.exception.UnableToCreateStatementException;
import org.jdbi.v3.core.exception.UnableToExecuteStatementException;
import org.jdbi.v3.core.exception.UnableToProduceResultException;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.SingleColumnMapper;
import org.jdbi.v3.core.rewriter.RewrittenStatement;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.StatementBuilder;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.jdbi.v3.core.util.GenericType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a prepared batch statement. That is, a sql statement compiled as a prepared
 * statement, and then executed multiple times in a single batch. This is, generally,
 * a very efficient way to execute large numbers of the same statement where
 * the statement only varies by the arguments bound to it.
 */
public class PreparedBatch extends SqlStatement<PreparedBatch> implements ResultBearing, ResultSetIterable
{
    private static final Logger LOG = LoggerFactory.getLogger(PreparedBatch.class);

    private final List<PreparedBatchPart> parts = new ArrayList<>();
    private Binding currentBinding;

    PreparedBatch(ConfigRegistry config,
                  Handle handle,
                  StatementBuilder statementBuilder,
                  String sql,
                  StatementContext ctx,
                  Collection<StatementCustomizer> statementCustomizers)
    {
        super(config, new Binding(), handle, statementBuilder, sql, ctx, statementCustomizers);
        this.currentBinding = new Binding();
    }

    @Override
    public <R> R withResultSet(ResultSetCallback<R> callback) {
        return execute(returningResults()).withResultSet(callback);
    }

    /**
     * Execute the batch
     *
     * @return the number of rows modified or inserted per batch part.
     */
    public int[] execute() {
        // short circuit empty batch
        if (parts.isEmpty()) {
            return new int[0];
        }

        return internalBatchExecute().updateCounts;
    }

    public ResultIterator<Integer> executeAndGetModCount() {
        StatementContext ctx = getContext();
        final int[] modCount = execute();
        return new ResultIterator<Integer>() {
            int pos = 0;
            @Override
            public boolean hasNext() {
                return pos < modCount.length;
            }

            @Override
            public Integer next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return modCount[pos++];
            }

            @Override
            public StatementContext getContext() {
                return ctx;
            }

            @Override
            public void close() {
            }
        };
    }

    public ResultSetIterable executeAndReturnGeneratedKeys(String... columnNames) {
        return execute(returningGeneratedKeys(columnNames));
    }

    @Override
    public <R> R execute(ResultProducer<R> producer) {
        if (parts.isEmpty()) {
            throw new IllegalStateException("No PreparedBatchParts to execute");
        }

        try {
            return producer.produce(() -> internalBatchExecute().stmt, getContext());
        } catch (SQLException e) {
            throw new UnableToProduceResultException("Exception producing batch result", e, getContext());
        }
    }

    private static class ExecutedBatch {
        final PreparedStatement stmt;
        final int[] updateCounts;

        ExecutedBatch(PreparedStatement stmt, int[] updateCounts) {
            this.stmt = stmt;
            this.updateCounts = updateCounts;
        }
    }

    private ExecutedBatch internalBatchExecute() {
        if (parts.isEmpty()) {
            throw new IllegalStateException("No PreparedBatchParts to execute");
        }

        RewrittenStatement rewritten = getConfig(SqlStatements.class)
                .getStatementRewriter()
                .rewrite(getSql(), parts.get(0).getParams(), getContext());

        try {
            final PreparedStatement stmt;
            String sql = rewritten.getSql();
            try {
                StatementBuilder statementBuilder = getStatementBuilder();
                Connection connection = getHandle().getConnection();
                stmt = statementBuilder.create(connection, sql, getContext());
                addCleanable(Cleanables.forStatementBuilder(statementBuilder, connection, sql, stmt));
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
                LOG.trace("Prepared batch of {} parts executed in {}ms", parts.size(), elapsedTime / 1000000L, sql);
                getConfig(SqlStatements.class).getTimingCollector().collect(elapsedTime, getContext());

                afterExecution(stmt);

                return new ExecutedBatch(stmt, rs);
            }
            catch (SQLException e) {
                throw new UnableToExecuteStatementException(Batch.mungeBatchException(e), getContext());
            }
        }
        finally {
            parts.clear();
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
        PreparedBatchPart part = new PreparedBatchPart(getConfig(),
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
        part.bindMap(args);
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

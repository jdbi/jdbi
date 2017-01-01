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
package org.jdbi.v3.core.statement;

import static org.jdbi.v3.core.result.ResultProducers.returningGeneratedKeys;
import static org.jdbi.v3.core.result.ResultProducers.returningResults;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.result.ResultBearing;
import org.jdbi.v3.core.result.ResultIterator;
import org.jdbi.v3.core.result.ResultProducer;
import org.jdbi.v3.core.result.ResultSetMapper;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.rewriter.RewrittenStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a prepared batch statement.  Multiple bindings are added to the
 * compiled statement and then executed in a single operation. This is, generally,
 * a very efficient way to execute large numbers of the same statement where
 * the statement only varies by the arguments bound to it.
 *
 * The statement starts with an empty binding.  You bind a single batch of parameters
 * with the usual {@link SqlStatement} binding methods, and then call
 * {@link PreparedBatch#add()} to add the current binding as a batch and then clear it.
 *
 * An entire batch can be bound and added in one go with {@link PreparedBatch#add(Map)}
 * or {@link PreparedBatch#add(Object...)}.
 */
public class PreparedBatch extends SqlStatement<PreparedBatch> implements ResultBearing
{
    private static final Logger LOG = LoggerFactory.getLogger(PreparedBatch.class);

    private final List<Binding> bindings = new ArrayList<>();

    public PreparedBatch(Handle handle, String sql)
    {
        super(handle, sql);
    }

    @Override
    public <R> R mapResultSet(ResultSetMapper<R> mapper) {
        return execute(returningResults()).mapResultSet(mapper);
    }

    /**
     * Execute the batch
     *
     * @return the number of rows modified or inserted per batch part.
     */
    public int[] execute() {
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

    public ResultBearing executeAndReturnGeneratedKeys(String... columnNames) {
        return execute(returningGeneratedKeys(columnNames));
    }

    /**
     * Executes the batch, returning the result obtained from the given {@link ResultProducer}.
     *
     * @param producer the result producer.
     * @return value returned by the result producer.
     */
    public <R> R execute(ResultProducer<R> producer) {
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
        if (!getBinding().isEmpty()) {
            add();
        }
        if (bindings.isEmpty()) {
            throw new IllegalStateException("No batch parts to execute");
        }

        RewrittenStatement rewritten = getConfig(SqlStatements.class)
                .getStatementRewriter()
                .rewrite(getSql(), bindings.get(0), getContext());

        try {
            final PreparedStatement stmt;
            String sql = rewritten.getSql();
            try {
                StatementBuilder statementBuilder = getHandle().getStatementBuilder();
                Connection connection = getHandle().getConnection();
                stmt = statementBuilder.create(connection, sql, getContext());
                addCleanable(() -> statementBuilder.close(connection, sql, stmt));
            }
            catch (SQLException e) {
                throw new UnableToCreateStatementException(e, getContext());
            }


            try {
                for (Binding binding : bindings) {
                    rewritten.bind(binding, stmt);
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
                LOG.trace("Prepared batch of {} parts executed in {}ms", bindings.size(), elapsedTime / 1000000L, sql);
                getConfig(SqlStatements.class).getTimingCollector().collect(elapsedTime, getContext());

                afterExecution(stmt);

                return new ExecutedBatch(stmt, rs);
            }
            catch (SQLException e) {
                throw new UnableToExecuteStatementException(Batch.mungeBatchException(e), getContext());
            }
        }
        finally {
            bindings.clear();
        }
    }

    /**
     * Add the current binding as a saved batch and clear the binding.
     */
    public PreparedBatch add()
    {
        bindings.add(getBinding());
        getContext().setBinding(new Binding());
        return this;
    }

    /**
     * Bind arguments positionally, add the binding as a saved batch, and
     * then clear the current binding.
     * @param args the positional arguments to bind
     * @return this
     */
    public PreparedBatch add(Object... args)
    {
        for(int i = 0; i < args.length; i++) {
            bind(i, args[i]);
        }
        add();
        return this;
    }

    /**
     * Bind arguments from a Map, add the binding as a saved batch,
     * then clear the current binding.
     *
     * @param args map to bind arguments from for named parameters on the statement
     * @return this
     */
    public PreparedBatch add(Map<String, ?> args)
    {
        bindMap(args);
        add();
        return this;
    }

    /**
     * @return the number of bindings which are in this batch
     */
    public int size()
    {
        return bindings.size();
    }
}

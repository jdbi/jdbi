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

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.argument.internal.NamedArgumentFinderFactory;
import org.jdbi.v3.core.argument.internal.NamedArgumentFinderFactory.PrepareKey;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.result.BatchResultBearing;
import org.jdbi.v3.core.result.ResultBearing;
import org.jdbi.v3.core.result.ResultIterator;
import org.jdbi.v3.core.result.ResultProducer;
import org.jdbi.v3.core.result.ResultProducers;
import org.jdbi.v3.core.result.ResultSetScanner;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.statement.internal.PreparedBinding;

import static org.jdbi.v3.core.result.ResultProducers.returningGeneratedKeys;

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
public class PreparedBatch extends SqlStatement<PreparedBatch> implements ResultBearing {
    private final List<PreparedBinding> bindings = new ArrayList<>();
    final Map<PrepareKey, Function<String, Optional<Function<Object, Argument>>>> preparedFinders = new HashMap<>();

    public PreparedBatch(Handle handle, CharSequence sql) {
        super(handle, sql);
        getContext().setBinding(new PreparedBinding(this, getContext()));
    }

    /**
     * Deprecated delegate - please use {@code CharSequence} signature for future compatibility.
     */
    public PreparedBatch(Handle handle, String sql) {
        super(handle, (CharSequence) sql);
    }

    @Override
    PreparedBatch bindNamedArgumentFinder(NamedArgumentFinderFactory factory, String prefix, Object value, Type type, Supplier<NamedArgumentFinder> backupArgumentFinder) {
        PreparedBinding binding = getBinding();
        PrepareKey key = factory.keyFor(prefix, value);
        preparedFinders.computeIfAbsent(key,
                pk -> factory.prepareFor(getConfig(), this::buildArgument, prefix, value, type));
        binding.prepareKeys.put(key, value);
        binding.backupArgumentFinders.add(backupArgumentFinder);
        return this;
    }

    @Override
    protected PreparedBinding getBinding() {
        return (PreparedBinding) super.getBinding();
    }

    Function<Object, Argument> buildArgument(QualifiedType<?> type) {
        return getContext().getConfig(Arguments.class)
                .prepareFor(type)
                .orElse(value ->
                    (pos, st, ctx) ->
                        ctx.getConfig(Arguments.class)
                            .findFor(type, value)
                            .orElseThrow(() -> new UnableToCreateStatementException("no argument factory for type " + type, ctx))
                            .apply(pos, st, ctx));
    }

    @Override
    public <R> R scanResultSet(ResultSetScanner<R> mapper) {
        return execute(ResultProducers.returningResults()).scanResultSet(mapper);
    }

    /**
     * Execute the batch and return the number of rows affected for each batch part.
     * Note that some database drivers might return special values like {@link Statement#SUCCESS_NO_INFO}
     * or {@link Statement#EXECUTE_FAILED}.
     *
     * @return the number of rows affected per batch part
     * @see Statement#executeBatch()
     */
    public int[] execute() {
        try {
            return internalBatchExecute().updateCounts;
        } finally {
            getContext().close();
        }
    }

    /**
     * Execute the batch and return the mod counts as in {@code execute}, but as a
     * Jdbi result iterator instead of an array.
     * @return the number of rows affected per batch part
     * @see #execute()
     * @see Statement#executeBatch()
     */
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
                ctx.close();
            }
        };
    }

    /**
     * Execute the batch and give access to any generated keys returned by the operation.
     *
     * @param columnNames The column names for generated keys.
     * @return A {@link ResultBearing} object that can be used to access the results of the batch.
     * @deprecated Use {@link #executePreparedBatch(String...)} which has the same functionality but also returns the per-batch modified row counts.
     */
    @Deprecated
    public ResultBearing executeAndReturnGeneratedKeys(String... columnNames) {
        return execute(returningGeneratedKeys(columnNames));
    }

    /**
     * Execute the batch and give access to any generated keys returned by the operation.
     *
     * @param columnNames The column names for generated keys.
     * @return A {@link BatchResultBearing} object that can be used to access the results of the batch and the per-batch modified row counts.
     */
    public BatchResultBearing executePreparedBatch(String... columnNames) {
        final ExecutedBatchConsumer executedBatchConsumer = new ExecutedBatchConsumer();
        final ResultBearing resultBearing = execute(returningGeneratedKeys(columnNames), executedBatchConsumer);
        return new BatchResultBearing(resultBearing, executedBatchConsumer);
    }

    /**
     * Executes the batch, returning the result obtained from the given {@link ResultProducer}.
     *
     * @param <R> the type of the result
     * @param producer the result producer.
     * @return value returned by the result producer.
     */
    public <R> R execute(ResultProducer<R> producer) {
        return execute(producer, x -> {});
    }

    private <R> R execute(ResultProducer<R> producer, Consumer<ExecutedBatch> batchConsumer) {
        try {
            return producer.produce(() -> {
                ExecutedBatch batch = internalBatchExecute();
                batchConsumer.accept(batch);
                return batch.stmt;
            }, getContext());
        } catch (SQLException e) {
            try {
                close();
            } catch (Exception e1) {
                e.addSuppressed(e1);
            }
            throw new UnableToProduceResultException("Exception producing batch result", e, getContext());
        }
    }

    private ExecutedBatch internalBatchExecute() {
        if (!getBinding().isEmpty()) {
            add();
        }

        beforeTemplating();

        final StatementContext ctx = getContext();

        ParsedSql parsedSql = parseSql();
        String sql = parsedSql.getSql();
        ParsedParameters parsedParameters = parsedSql.getParameters();

        try {
            try {
                StatementBuilder statementBuilder = getHandle().getStatementBuilder();
                @SuppressWarnings("PMD.CloseResource")
                Connection connection = getHandle().getConnection();
                stmt = statementBuilder.create(connection, sql, ctx);

                addCleanable(() -> statementBuilder.close(connection, sql, stmt));
                getConfig(SqlStatements.class).customize(stmt);
            } catch (SQLException e) {
                throw new UnableToCreateStatementException(e, ctx);
            }

            if (bindings.isEmpty()) {
                return new ExecutedBatch(stmt, new int[0]);
            }

            beforeBinding();

            try {
                ArgumentBinder binder = new ArgumentBinder(stmt, ctx, parsedParameters);
                for (Binding binding : bindings) {
                    ctx.setBinding(binding);
                    binder.bind(binding);
                    stmt.addBatch();
                }
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException("Exception while binding parameters", e, ctx);
            }

            beforeExecution();

            try {
                final int[] modifiedRows = SqlLoggerUtil.wrap(stmt::executeBatch, ctx, getConfig(SqlStatements.class).getSqlLogger());

                afterExecution();

                ctx.setBinding(new PreparedBinding(this, ctx));

                return new ExecutedBatch(stmt, modifiedRows);
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException(Batch.mungeBatchException(e), ctx);
            }
        } finally {
            bindings.clear();
        }
    }

    /**
     * Add the current binding as a saved batch and clear the binding.
     * @return this
     */
    public PreparedBatch add() {
        final PreparedBinding currentBinding = getBinding();
        if (currentBinding.isEmpty()) {
            throw new IllegalStateException("Attempt to add() an empty batch, you probably didn't mean to do this "
                    + "- call add() *after* setting batch parameters");
        }
        bindings.add(currentBinding);
        getContext().setBinding(new PreparedBinding(this, getContext()));
        return this;
    }

    /**
     * Bind arguments positionally, add the binding as a saved batch, and
     * then clear the current binding.
     * @param args the positional arguments to bind
     * @return this
     */
    public PreparedBatch add(Object... args) {
        for (int i = 0; i < args.length; i++) {
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
    public PreparedBatch add(Map<String, ?> args) {
        bindMap(args);
        add();
        return this;
    }

    /**
     * Returns the number of bindings in this batch.
     *
     * @return the number of bindings in this batch.
     */
    public int size() {
        return bindings.size();
    }

    private static class ExecutedBatch {
        final PreparedStatement stmt;
        final int[] updateCounts;

        ExecutedBatch(PreparedStatement stmt, int[] updateCounts) {
            this.stmt = stmt;
            this.updateCounts = Arrays.copyOf(updateCounts, updateCounts.length);
        }
    }

    private static final class ExecutedBatchConsumer implements Consumer<ExecutedBatch>, Supplier<int[]> {

        private int[] modifiedRowCounts = new int[0];

        @Override
        public void accept(ExecutedBatch executedBatch) {
            // has been copied within the executed batch
            this.modifiedRowCounts = executedBatch.updateCounts;
        }

        @Override
        @SuppressWarnings("PMD.MethodReturnsInternalArray")
        public int[] get() {
            // Array was copied as when the ExecutedBatch was created, so exposing it is fine.
            return modifiedRowCounts;
        }
    }
}

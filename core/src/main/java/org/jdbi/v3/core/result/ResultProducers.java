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
package org.jdbi.v3.core.result;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Commonly used ResultProducer implementations.
 */
public class ResultProducers implements JdbiConfig<ResultProducers> {

    private boolean allowNoResults = false;

    public ResultProducers() {}

    private ResultProducers(ResultProducers that) {
        this.allowNoResults = that.allowNoResults;
    }

    /**
     * Result producer that eagerly executes the statement, returning the update count
     *
     * @return update count
     * @see PreparedStatement#getUpdateCount()
     */
    public static ResultProducer<Integer> returningUpdateCount() {
        return (statementSupplier, ctx) -> {
            // suppress exception if ctx.close method raises
            try (StatementContext context = ctx) {
                return statementSupplier.get().getUpdateCount();
            }
        };
    }

    /**
     * Result producer that returns a {@link ResultBearing} over the statement result rows.
     *
     * @return ResultBearing of result rows.
     * @see PreparedStatement#getResultSet()
     */
    public static ResultProducer<ResultBearing> returningResults() {
        return (statementSupplier, ctx) -> createResultBearing(statementSupplier, Statement::getResultSet, ctx);
    }

    /**
     * Result producer that returns a {@link ResultBearing} over the statement-generated keys.
     *
     * @param generatedKeyColumnNames optional list of generated key column names.
     * @return ResultBearing of generated keys
     * @see PreparedStatement#getGeneratedKeys()
     */
    public static ResultProducer<ResultBearing> returningGeneratedKeys(String... generatedKeyColumnNames) {
        return (preparedStatementSupplier, ctx) -> {
            ctx.setReturningGeneratedKeys(true);

            if (generatedKeyColumnNames.length > 0) {
                ctx.setGeneratedKeysColumnNames(generatedKeyColumnNames);
            }

            return createResultBearing(preparedStatementSupplier, Statement::getGeneratedKeys, ctx);
        };
    }

    /**
     * Create a {@link ResultBearing} instance backed by a {@link ResultSet}. This method can be used to create other {@link ResultProducer} instances
     * that manage a {@link ResultBearing} instance.
     *
     * @param preparedStatementSupplier Provides the {@link PreparedStatement} to obtain the {@link ResultSet}.
     * @param resultSetCreator Creates a {@link ResultSet} from a {@link Statement}.
     * @param ctx The statement context.
     * @return An instance of {@link ResultBearing} that is backed by the {@link ResultSet}.
     */
    public static ResultBearing createResultBearing(Supplier<PreparedStatement> preparedStatementSupplier, ResultSetCreator resultSetCreator, StatementContext ctx) {
        return ResultBearing.of(() -> {
            try {
                ResultSet resultSet = resultSetCreator.createResultSet(preparedStatementSupplier.get());

                if (resultSet == null) {
                    if (ctx.getConfig(ResultProducers.class).allowNoResults) {
                        return new EmptyResultSet();
                    }
                    throw new NoResultsException("Statement returned no results", ctx);
                }

                ctx.addCleanable(resultSet::close);

                return resultSet;
            } catch (SQLException e) {
                throw new ResultSetException("Could not process result set", e, ctx);
            }
        }, ctx);
    }

    @Override
    public ResultProducers createCopy() {
        return new ResultProducers(this);
    }

    /**
     * Normally a query that doesn't return a result set throws an exception.
     * With this option, we will replace it with an empty result set instead.
     *
     * @param allowNoResults True if an empty {@link ResultSet} object should be returned, false if a {@link NoResultsException} should be thrown.
     * @return this
     */
    public ResultProducers allowNoResults(boolean allowNoResults) {
        this.allowNoResults = allowNoResults;
        return this;
    }

    @FunctionalInterface
    /**
     * Returns a ResultSet from a Statement.
     */
    public interface ResultSetCreator {

        /**
         * Use the supplied statement to create a ResultSet.
         * @param statement An implementation of {@link Statement}.
         * @return A {@link ResultSet}. May be null.
         * @throws SQLException If the result set could not be created.
         */
        ResultSet createResultSet(Statement statement) throws SQLException;
    }
}

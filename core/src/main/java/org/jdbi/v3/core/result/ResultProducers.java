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
        return (supplier, ctx) -> ResultBearing.of(getResultSet(supplier, ctx), ctx);
    }

    private static Supplier<ResultSet> getResultSet(Supplier<PreparedStatement> supplier, StatementContext ctx) {
        return () -> {
            try {
                ResultSet rs = supplier.get().getResultSet();

                if (rs == null) {
                    if (ctx.getConfig(ResultProducers.class).allowNoResults) {
                        return new EmptyResultSet();
                    }
                    throw new NoResultsException("Statement returned no results", ctx);
                }

                ctx.addCleanable(rs::close);

                return rs;
            } catch (SQLException e) {
                throw new ResultSetException("Could not get result set", e, ctx);
            }
        };
    }

    /**
     * Result producer that returns a {@link ResultBearing} over the statement-generated keys.
     *
     * @param generatedKeyColumnNames optional list of generated key column names.
     * @return ResultBearing of generated keys
     * @see PreparedStatement#getGeneratedKeys()
     */
    public static ResultProducer<ResultBearing> returningGeneratedKeys(String... generatedKeyColumnNames) {
        return (supplier, ctx) -> {
            ctx.setReturningGeneratedKeys(true);

            if (generatedKeyColumnNames.length > 0) {
                ctx.setGeneratedKeysColumnNames(generatedKeyColumnNames);
            }

            return ResultBearing.of(getGeneratedKeys(supplier, ctx), ctx);
        };
    }

    private static Supplier<ResultSet> getGeneratedKeys(Supplier<PreparedStatement> supplier, StatementContext ctx) {
        return () -> {
            try {
                ResultSet rs = supplier.get().getGeneratedKeys();

                if (rs == null) {
                    throw new NoResultsException("Statement returned no generated keys", ctx);
                }

                ctx.addCleanable(rs::close);

                return rs;
            } catch (SQLException e) {
                throw new ResultSetException("Could not get generated keys", e, ctx);
            }
        };
    }

    @Override
    public ResultProducers createCopy() {
        return new ResultProducers(this);
    }

    /**
     * Normally a query that doesn't return a result set throws an exception.
     * With this option, we will replace it with an empty result set instead.
     * @param allowed the new allowNoResults setting
     * @return this
     */
    public ResultProducers allowNoResults(boolean allowed) {
        this.allowNoResults = allowed;
        return this;
    }
}

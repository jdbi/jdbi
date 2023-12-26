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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.internal.exceptions.Sneaky;
import org.jdbi.v3.core.result.ResultBearing;
import org.jdbi.v3.core.result.internal.ResultSetSupplier;

/**
 * Used for invoking stored procedures. The most common way to use this is to register {@link OutParameters} with the call and then use the {@link Call#invoke()} method
 * to retrieve the return values from the invoked procedure.
 * <br>
 * There are some databases, most prominently MS SqlServer that only support limited OUT parameters, especially do not support cursors
 * (for MS SqlServer see <a href="https://learn.microsoft.com/en-us/sql/connect/jdbc/using-a-stored-procedure-with-output-parameters">Using a stored procedure with output parameters</a>).
 * Those databases may support returning a result set from the procedure invocation, to access this result set use the {@link OutParameters#getResultSet()} method to retrieve
 * the result set from the underlying call operation.
 */
public class Call extends SqlStatement<Call> {
    private final List<OutParamArgument> outParamArguments = new ArrayList<>();

    public Call(Handle handle, CharSequence sql) {
        super(handle, sql);
    }

    /**
     * Backwards compatible constructor that takes an explicit string argument.
     *
     * @see Call#Call(Handle, CharSequence)
     */
    public Call(Handle handle, String sql) {
        super(handle, sql);
    }

    @Override
    PreparedStatement createStatement(String parsedSql) throws SQLException {
        return getHandle().getStatementBuilder().createCall(getHandle().getConnection(), parsedSql, getContext());
    }

    /**
     * Register a positional output parameter.
     * @param position the parameter position (zero-based)
     * @param sqlType an SQL type constant as defined by {@link java.sql.Types} or by the JDBC vendor.
     * @return self
     */
    public Call registerOutParameter(int position, int sqlType) {
        return registerOutParameter(position, sqlType, null);
    }

    /**
     * Register a positional output parameter.
     * @param position the parameter position (zero-based)
     * @param sqlType an SQL type constant as defined by {@link java.sql.Types} or by the JDBC vendor.
     * @param mapper a mapper which converts the {@link CallableStatement} to a desired output type.
     * @return self
     */
    public Call registerOutParameter(int position, int sqlType, CallableStatementMapper mapper) {
        getBinding().addPositional(position, new OutParamArgument(sqlType, mapper, null));
        return this;
    }

    /**
     * Register a named output parameter.
     * @param name the parameter name
     * @param sqlType an SQL type constant as defined by {@link java.sql.Types} or by the JDBC vendor.
     * @return self
     */
    public Call registerOutParameter(String name, int sqlType) {
        return registerOutParameter(name, sqlType, null);
    }

    /**
     * Register a named output parameter.
     * @param name the parameter name
     * @param sqlType an SQL type constant as defined by {@link java.sql.Types} or by the JDBC vendor.
     * @param mapper a mapper which converts the {@link CallableStatement} to a desired output type.
     * @return self
     */
    public Call registerOutParameter(String name, int sqlType, CallableStatementMapper mapper) {
        getBinding().addNamed(name, new OutParamArgument(sqlType, mapper, name));
        return this;
    }

    /**
     * Invoke the callable statement.  Note that the statement will be cleaned up, so cursor-typed values may not work.
     *
     * @return the output parameters resulting from the invocation.
     */
    public OutParameters invoke() {
        return invoke(Function.identity());
    }

    /**
     * Invoke the callable statement and process its {@link OutParameters} results.
     */
    public void invoke(Consumer<OutParameters> resultConsumer) {
        invoke((Function<OutParameters, Void>) r -> {
            resultConsumer.accept(r);
            return null;
        });
    }

    /**
     * Invoke the callable statement and process its {@link OutParameters} results,
     * returning a computed value of type {@code T}.
     */
    public <T> T invoke(Function<OutParameters, T> resultComputer) {
        // it is ok to ignore the PreparedStatement returned here. internalExecute registers it to close with the context and the
        // nullSafeCleanUp below will take care of it.
        internalExecute();

        final Supplier<ResultSet> resultSetSupplier = () -> {
            try {
                return stmt.getResultSet();
            } catch (SQLException e) {
                throw Sneaky.throwAnyway(e);
            }
        };


        final ResultBearing resultSet = ResultBearing.of(ResultSetSupplier.notClosingContext(resultSetSupplier), getContext());

        OutParameters out = new OutParameters(resultSet, getContext());

        outParamArguments.forEach(outparamArgument -> {
            Supplier<Object> supplier = outparamArgument.supplier((CallableStatement) stmt);
            // index is 0 based, position is 1 based.
            out.setValue(outparamArgument.position - 1, outparamArgument.name, supplier);
        });

        return resultComputer.apply(out);
    }

    /**
     * Specify the fetch size for the call. This should cause the results to be
     * fetched from the underlying RDBMS in groups of rows equal to the number passed.
     * This is useful for doing chunked streaming of results when exhausting memory
     * could be a problem.
     *
     * @param fetchSize the number of rows to fetch in a bunch
     *
     * @return the modified call
     */
    public Call setFetchSize(final int fetchSize) {
        return addCustomizer(StatementCustomizers.fetchSize(fetchSize));
    }

    /**
     * Specify the maximum number of rows the call is to return. This uses the underlying JDBC
     * {@link Statement#setMaxRows(int)}}.
     *
     * @param maxRows maximum number of rows to return
     *
     * @return modified call
     */
    public Call setMaxRows(final int maxRows) {
        return addCustomizer(StatementCustomizers.maxRows(maxRows));
    }

    /**
     * Specify the maximum field size in the result set. This uses the underlying JDBC
     * {@link Statement#setMaxFieldSize(int)}
     *
     * @param maxFields maximum field size
     *
     * @return modified call
     */
    public Call setMaxFieldSize(final int maxFields) {
        return addCustomizer(StatementCustomizers.maxFieldSize(maxFields));
    }

    /**
     * Specify that the result set should be concurrent updatable.
     *
     * This will allow the update methods to be called on the result set produced by this
     * Call.
     *
     * @return the modified call
     */
    public Call concurrentUpdatable() {
        getContext().setConcurrentUpdatable(true);
        return this;
    }

    // TODO tostring?
    private class OutParamArgument implements Argument {
        private final int sqlType;
        private final CallableStatementMapper mapper;
        private final String name;
        private int position;

        OutParamArgument(int sqlType, CallableStatementMapper mapper, String name) {
            this.sqlType = sqlType;
            this.mapper = mapper;
            this.name = name;
            outParamArguments.add(this);
        }

        @Override
        public void apply(int outPosition, PreparedStatement statement, StatementContext ctx) throws SQLException {
            ((CallableStatement) statement).registerOutParameter(outPosition, sqlType);
            this.position = outPosition;
        }

        public Supplier<Object> supplier(CallableStatement stmt) {
            return () -> {
                Object value = map(stmt);
                return isNull(stmt) ? null : value;
            };
        }

        private Object map(CallableStatement stmt) {
            try {
                if (mapper != null) {
                    return mapper.map(position, stmt);
                }
                switch (sqlType) {
                    case Types.CLOB:
                    case Types.VARCHAR:
                    case Types.LONGNVARCHAR:
                    case Types.LONGVARCHAR:
                    case Types.NCLOB:
                    case Types.NVARCHAR:
                        return stmt.getString(position);
                    case Types.BLOB:
                    case Types.VARBINARY:
                        return stmt.getBytes(position);
                    case Types.SMALLINT:
                        return stmt.getShort(position);
                    case Types.INTEGER:
                        return stmt.getInt(position);
                    case Types.BIGINT:
                        return stmt.getLong(position);
                    case Types.TIMESTAMP:
                    case Types.TIME:
                        return stmt.getTimestamp(position);
                    case Types.DATE:
                        return stmt.getDate(position);
                    case Types.FLOAT:
                        return stmt.getFloat(position);
                    case Types.DECIMAL:
                    case Types.DOUBLE:
                        return stmt.getDouble(position);
                    default:
                        return stmt.getObject(position);
                }
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException("Could not get OUT parameter from statement", e, getContext());
            }
        }

        private boolean isNull(CallableStatement stmt) {
            try {
                return stmt.wasNull();
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException("Could not get OUT parameter from statement", e, getContext());
            }
        }
    }
}

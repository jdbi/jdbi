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
package org.jdbi.oracle12;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import oracle.jdbc.OraclePreparedStatement;
import org.jdbi.core.argument.Argument;
import org.jdbi.core.result.ResultBearing;
import org.jdbi.core.result.ResultProducer;
import org.jdbi.core.result.ResultProducers;
import org.jdbi.core.statement.Binding;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.core.statement.StatementCustomizer;
import org.jdbi.meta.Beta;

/**
 * Returns a {@link ResultBearing} from Oracle's "DML Returning" features introduced in 10.2. To use,
 * add a {@link #returnParameters()} customizer to the statement and register with one or more return parameters. Then
 * execute the statement with {@link #returningDml()} result producer:
 * <p>
 * <pre>
 * List&lt;Integer&gt; ids = handle.createUpdate("insert into something (id, name) values (17, 'Brian') returning id into ?")
 *     .addCustomizer(OracleReturning.returnParameters().register(0, OracleTypes.INTEGER))
 *     .execute(OracleReturning.returningDml())
 *     .mapTo(int.class)
 *     .list();
 *
 * assertThat(ids).containsExactly(17);
 * </pre>
 * <p>
 * This class still is beta, and may be changed incompatibly or removed at any time.
 */
@Beta
public class OracleReturning {

    private OracleReturning() {}

    public static ReturnParameters returnParameters() {
        return new ReturnParameters();
    }

    /**
     * Result producer that returns a {@link ResultBearing} over the statement "DML returning" parameters. Used in
     * conjunction with {@link #returnParameters()} to register return parameters.
     *
     * @return ResultBearing of returned columns.
     * @see OraclePreparedStatement#getReturnResultSet()
     */
    public static ResultProducer<ResultBearing> returningDml() {
        return (supplier, ctx) -> ResultProducers.createResultBearing(supplier, statement -> unwrapOracleStatement(statement).getReturnResultSet(), ctx);
    }

    private static OraclePreparedStatement unwrapOracleStatement(Statement stmt) throws SQLException {
        if (!stmt.isWrapperFor(OraclePreparedStatement.class)) {
            throw new IllegalStateException("Statement is not an instance of, nor a wrapper of, OraclePreparedStatement");
        }
        return stmt.unwrap(OraclePreparedStatement.class);
    }

    static class ReturnParam implements Argument {

        private final String name;
        private final int index;
        private final int oracleType;

        ReturnParam(String name, int oracleType) {
            this.name = name;
            this.index = -1;
            this.oracleType = oracleType;
        }

        ReturnParam(int index, int oracleType) {
            this.name = null;
            this.index = index;
            this.oracleType = oracleType;
        }

        @Override
        public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
            unwrapOracleStatement(statement).registerReturnParameter(position, oracleType);
        }

        void bind(Binding binding) {
            if (name == null) {
                binding.addPositional(index, this);
            } else {
                binding.addNamed(name, this);
            }
        }
    }

    public static class ReturnParameters implements StatementCustomizer {

        private final List<ReturnParam> returnParams = new ArrayList<>();

        ReturnParameters() {}

        @Override
        public void beforeBinding(PreparedStatement stmt, StatementContext ctx) throws SQLException {
            for (ReturnParam returnParam : returnParams) {
                returnParam.bind(ctx.getBinding());
            }
        }

        /**
         * Registers a return parameter on the Oracle prepared statement.
         *
         * @param index      0-based index of the return parameter
         * @param oracleType one of the values from {@link oracle.jdbc.OracleTypes}
         * @return The same instance, for method chaining
         */
        public ReturnParameters register(int index, int oracleType) {
            returnParams.add(new ReturnParam(index, oracleType));
            return this;
        }

        /**
         * Registers a return parameter on the Oracle prepared statement.
         *
         * @param name       name of the return parameter
         * @param oracleType one of the values from {@link oracle.jdbc.OracleTypes}
         * @return The same instance, for method chaining
         */
        public ReturnParameters register(String name, int oracleType) {
            returnParams.add(new ReturnParam(name, oracleType));
            return this;
        }
    }
}

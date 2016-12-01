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
package org.jdbi.v3.oracle;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.exception.ResultSetException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.statement.StatementCustomizer;

import oracle.jdbc.OraclePreparedStatement;

/**
 * BETA: Provides access to Oracle's "DML Returning" features introduced in 10.2. To use,
 * add the statement customizer to a DML statement and bind (positionally) the return
 * params. Sadly, I think they (and the mapper) have to be positional. Usage is
 * like this:
 * <pre>
 * public void testFoo() throws Exception
 * {
 *     Handle h = dbi.open();
 *
 *     OracleReturning&lt;Integer&gt; or = new OracleReturning&lt;Integer&gt;(new RowMapper&lt;Integer&gt;() {
 *         public Integer map(int index, ResultSet r) throws SQLException
 *         {
 *             return r.getInt(1);
 *         }
 *     });
 *
 *     or.registerReturnParam(1, OracleTypes.INTEGER);
 *
 *     h.createStatement("insert into something (id, name) values (17, 'Brian') returning id into ?")
 *             .addStatementCustomizer(or)
 *             .execute();
 *     List&lt;Integer&gt; ids = or.getReturnedResults();
 *
 *     assertEquals(1, ids.size());
 *     assertEquals(Integer.valueOf(17), ids.getAttribute(0));
 *     h.close();
 * }
 * </pre>
 * Though you can bind multiple params, and whatnot
 * <p>
 * This class is beta, and may be changed incompatibly or removed at any time.
 */
public class OracleReturning<ResultType> implements StatementCustomizer {
    private final RowMapper<ResultType> mapper;
    private final List<int[]> binds = new ArrayList<>();
    private StatementContext context;
    private List<ResultType> results;
    private OraclePreparedStatement stmt;

    /**
     * Provide a mapper which knows how to do positional access, sadly the
     * {@link BeanMapper} uses the names in the result set
     *
     * @param mapper Must use only positional access to the result set
     */
    public OracleReturning(RowMapper<ResultType> mapper) {
        this.mapper = mapper;
    }

    /**
     * @see StatementCustomizer#beforeExecution(java.sql.PreparedStatement, StatementContext)
     */
    @Override
    public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException {
        this.context = ctx;

        if (stmt instanceof OraclePreparedStatement) {
            this.stmt = OraclePreparedStatement.class.cast(stmt);
        } else {
            try {
                // If it's an instance of Apache commons-dbcp DelegatingPreparedStatement
                final Method getDelegate = stmt.getClass().getMethod("getDelegate");
                final Object candidate = getDelegate.invoke(stmt);
                if (candidate instanceof OraclePreparedStatement) {
                    this.stmt = OraclePreparedStatement.class.cast(stmt);
                } else {
                    throw new Exception("Obtained delegate, but it still wasn't an OraclePreparedStatement");
                }
            } catch (Exception e) {
                throw new IllegalStateException("Statement is not an OraclePreparedStatement, nor " +
                        "one which we know how to find it from", e);
            }
        }
        for (int[] bind : binds) {
            try {
                this.stmt.registerReturnParameter(bind[0], bind[1]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void afterExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException {
        ResultSet rs;
        try {
            rs = this.stmt.getReturnResultSet();
        } catch (Exception e) {
            throw new ResultSetException("Unable to retrieve return result set", e, ctx);
        }
        this.results = new ArrayList<>();
        try {
            RowMapper<ResultType> mapper = this.mapper.specialize(rs, context);
            while (rs.next()) {
                results.add(mapper.map(rs, context));
            }
        } catch (SQLException e) {
            throw new ResultSetException("Unable to retrieve results from returned result set", e, ctx);
        }
    }


    /**
     * Callable after the statement has been executed to obtain the mapped results.
     *
     * @return the mapped results
     */
    public List<ResultType> getReturnedResults() {
        return results;
    }

    /**
     * Used to specify the types (required) of the out parameters. You must
     * register each of them.
     *
     * @param position   1 based position of the out parameter
     * @param oracleType one of the values from oracle.jdbc.driver..OracleTypes
     * @return The same instance, in case you want to chain things
     */
    public OracleReturning<ResultType> registerReturnParam(int position, int oracleType) {
        binds.add(new int[]{position, oracleType});
        return this;
    }
}

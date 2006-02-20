/* Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.ResultSetException;
import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.ReWrittenStatement;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.tweak.StatementRewriter;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Query<ResultType>
{
    private final StatementRewriter statementRewriter;
    private final Connection connection;
    private final String sql;
    private final ResultSetMapper<ResultType> mapper;
    private Parameters params;

    Query(Parameters params,
          ResultSetMapper<ResultType> mapper,
          StatementRewriter statementRewriter,
          Connection connection,
          String sql)
    {
        this.params = params;
        this.mapper = mapper;
        this.statementRewriter = statementRewriter;
        this.connection = connection;
        this.sql = sql;
    }

    Query(ResultSetMapper<ResultType> mapper, StatementRewriter statementRewriter, Connection connection, String sql)
    {
        this(new Parameters(), mapper, statementRewriter, connection, sql);
    }

    /**
     * Executes the query
     * <p>
     * Will eagerly load all results
     *
     * @return
     * @throws UnableToCreateStatementException
     *                            if there is an error creating the statement
     * @throws UnableToExecuteStatementException
     *                            if there is an error executing the statement
     * @throws ResultSetException if there is an error dealing with the result set
     */
    public List<ResultType> list()
    {
        return this.internalExecute(QueryPreperator.NO_OP, new QueryResultMunger<List<ResultType>>()
        {
            public List<ResultType> munge(ResultSet rs) throws SQLException
            {
                List<ResultType> result_list = new ArrayList<ResultType>();
                int index = 0;
                while (rs.next())
                {
                    result_list.add(mapper.map(index++, rs));
                }
                return result_list;
            }
        }, QueryPostMungeCleanup.CLOSE_RESOURCES);
    }

    /**
     * Executes the query.
     * <p>
     * Specifies a maximum of one result on the JDBC statement, and map that one result
     * as the return value, or return null if there is nothing in the results
     *
     * @return first result, mapped, or null if there is no first result
     */
    public ResultType first()
    {
        return this.internalExecute(QueryPreperator.MAX_ROWS_ONE, new QueryResultMunger<ResultType>()
        {
            public final ResultType munge(final ResultSet rs) throws SQLException
            {
                if (rs.next())
                {
                    return mapper.map(0, rs);
                }
                else
                {
                    // no result matches
                    return null;
                }
            }
        }, QueryPostMungeCleanup.CLOSE_RESOURCES);
    }

    private <Result> Result internalExecute(final QueryPreperator prep,
                                            final QueryResultMunger<Result> munger,
                                            QueryPostMungeCleanup cleanup)
    {
        ReWrittenStatement rewritten = statementRewriter.rewrite(sql, params);
        final PreparedStatement stmt;
        try
        {
            stmt = connection.prepareStatement(rewritten.getSql());
        }
        catch (SQLException e)
        {
            throw new UnableToCreateStatementException(e);
        }
        try
        {
            rewritten.bind(params, stmt);
        }
        catch (SQLException e)
        {
            throw new UnableToExecuteStatementException("Unable to bind parameters to query", e);
        }

        try
        {
            prep.prepare(stmt);
        }
        catch (SQLException e)
        {
            throw new UnableToExecuteStatementException("Unable to configure JDBC statement to 1", e);
        }

        ResultSet rs;
        try
        {
            rs = stmt.executeQuery();
        }
        catch (SQLException e)
        {
            throw new UnableToExecuteStatementException(e);
        }

        try
        {
            return munger.munge(rs);
        }
        catch (SQLException e)
        {
            throw new ResultSetException("Exception thrown while attempting to traverse the result set", e);
        }
        finally
        {
            cleanup.cleanup(this, stmt, rs);
        }
    }

    public <Type> Query<Type> map(Class<Type> resultType)
    {
        return this.map(new BeanMapper<Type>(resultType));
    }

    public <T> Query<T> map(ResultSetMapper<T> mapper)
    {
        return new Query<T>(params, mapper, statementRewriter, connection, sql);
    }

    /* position param stuff */

    /**
     * Used if you need to have some exotic parameter bound.
     *
     * @param position position to bind this argument, starting at 0
     * @param argument exotic argument factory
     * @return the same Query instance
     */
    public Query<ResultType> setArgument(int position, Argument argument)
    {
        params.addPositional(position, argument);
        return this;
    }

    /**
     * Used if you need to have some exotic parameter bound.
     *
     * @param name     name to bind this argument
     * @param argument exotic argument factory
     * @return the same Query instance
     */
    public Query<ResultType> setArgument(String name, Argument argument)
    {
        params.addNamed(name, argument);
        return this;
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setString(int position, String value)
    {
        return setArgument(position, new StringArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setString(String name, String value)
    {
        return setArgument(name, new StringArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setInteger(int position, int value)
    {
        return setArgument(position, new IntegerArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setInteger(String name, int value)
    {
        return setArgument(name, new IntegerArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @param length   how long is the stream being bound?
     * @return the same Query instance
     */
    public Query<ResultType> setAsciiStream(int position, InputStream value, int length)
    {
        return setArgument(position, new InputStreamArgument(value, length, true));
    }

    /**
     * Bind an argument by name
     *
     * @param name   token name to bind the paramater to
     * @param value  to bind
     * @param length bytes to read from value
     * @return the same Query instance
     */
    public Query<ResultType> setAsciiStream(String name, InputStream value, int length)
    {
        return setArgument(name, new InputStreamArgument(value, length, true));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setBigDecimal(int position, BigDecimal value)
    {
        return setArgument(position, new BigDecimalArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setBigDecimal(String name, BigDecimal value)
    {
        return setArgument(name, new BigDecimalArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setBinaryStream(int position, InputStream value, int length)
    {
        return setArgument(position, new InputStreamArgument(value, length, false));
    }

    /**
     * Bind an argument by name
     *
     * @param name   token name to bind the paramater to
     * @param value  to bind
     * @param length bytes to read from value
     * @return the same Query instance
     */
    public Query<ResultType> setBinaryStream(String name, InputStream value, int length)
    {
        return setArgument(name, new InputStreamArgument(value, length, false));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setBlob(int position, Blob value)
    {
        return setArgument(position, new BlobArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setBlob(String name, Blob value)
    {
        return setArgument(name, new BlobArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setBoolean(int position, boolean value)
    {
        return setArgument(position, new BooleanArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setBoolean(String name, boolean value)
    {
        return setArgument(name, new BooleanArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setByte(int position, byte value)
    {
        return setArgument(position, new ByteArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setByte(String name, byte value)
    {
        return setArgument(name, new ByteArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setBytes(int position, byte[] value)
    {
        return setArgument(position, new ByteArrayArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setBytes(String name, byte[] value)
    {
        return setArgument(name, new ByteArrayArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @param length   number of characters to read
     * @return the same Query instance
     */
    public Query<ResultType> setCharacterStream(int position, Reader value, int length)
    {
        return setArgument(position, new CharacterStreamArgument(value, length));
    }

    /**
     * Bind an argument by name
     *
     * @param name   token name to bind the paramater to
     * @param value  to bind
     * @param length number of characters to read
     * @return the same Query instance
     */
    public Query<ResultType> setCharacterStream(String name, Reader value, int length)
    {
        return setArgument(name, new CharacterStreamArgument(value, length));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setClob(int position, Clob value)
    {
        return setArgument(position, new ClobArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setClob(String name, Clob value)
    {
        return setArgument(name, new ClobArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setDate(int position, java.sql.Date value)
    {
        return setArgument(position, new SqlDateArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setDate(String name, java.sql.Date value)
    {
        return setArgument(name, new SqlDateArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setDate(int position, java.util.Date value)
    {
        return setArgument(position, new JavaDateArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setDate(String name, java.util.Date value)
    {
        return setArgument(name, new JavaDateArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setDouble(int position, Double value)
    {
        return setArgument(position, new DoubleArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setDouble(String name, Double value)
    {
        return setArgument(name, new DoubleArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setFloat(int position, Float value)
    {
        return setArgument(position, new FloatArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setFloat(String name, Float value)
    {
        return setArgument(name, new FloatArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setLong(int position, long value)
    {
        return setArgument(position, new LongArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setLong(String name, long value)
    {
        return setArgument(name, new LongArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setObject(int position, Object value)
    {
        return setArgument(position, new ObjectArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setObject(String name, Object value)
    {
        return setArgument(name, new ObjectArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setTime(int position, Time value)
    {
        return setArgument(position, new TimeArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setTime(String name, Time value)
    {
        return setArgument(name, new TimeArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setTimestamp(int position, Timestamp value)
    {
        return setArgument(position, new TimestampArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setTimestamp(String name, Timestamp value)
    {
        return setArgument(name, new TimestampArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public Query<ResultType> setUrl(int position, URL value)
    {
        return setArgument(position, new URLArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the paramater to
     * @param value to bind
     * @return the same Query instance
     */
    public Query<ResultType> setUrl(String name, URL value)
    {
        return setArgument(name, new URLArgument(value));
    }
}

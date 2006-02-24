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

public abstract class SQLStatement<SelfType extends SQLStatement>
{
    private final Parameters params;
    private final Connection connection;
    private final String sql;
    private final StatementRewriter rewriter;

    SQLStatement(Parameters params,
                 StatementRewriter rewriter,
                 Connection conn,
                 String sql)
    {
        assert(verifyOurNastyDowncastIsOkay());

        this.rewriter = rewriter;
        this.connection = conn;
        this.sql = sql;
        this.params = params;
    }

    private boolean verifyOurNastyDowncastIsOkay()
    {
        if (this.getClass().getTypeParameters().length == 0)
        {
            return true;
        }
        else
        {
            Class parameterized_type =
                    this.getClass().getTypeParameters()[0].getGenericDeclaration();
            return parameterized_type.isAssignableFrom(this.getClass());
        }
    }

    protected StatementRewriter getRewriter()
    {
        return rewriter;
    }

    protected Parameters getParams()
    {
        return params;
    }

    protected Connection getConnection()
    {
        return connection;
    }

    protected String getSql()
    {
        return sql;
    }

    protected Parameters getParameters()
    {
        return params;
    }

    /**
     * Used if you need to have some exotic parameter bound.
     *
     * @param position position to bind this argument, starting at 0
     * @param argument exotic argument factory
     * @return the same Query instance
     */
    @SuppressWarnings("unchecked")
    public SelfType setArgument(int position, Argument argument)
    {
        params.addPositional(position, argument);
        return (SelfType) this;
    }

    /**
     * Used if you need to have some exotic parameter bound.
     *
     * @param name     name to bind this argument
     * @param argument exotic argument factory
     * @return the same Query instance
     */
    @SuppressWarnings("unchecked")
    public SelfType setArgument(String name, Argument argument)
    {
        params.addNamed(name, argument);
        return (SelfType) this;
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public final SelfType setString(int position, String value)
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
    public final SelfType setString(String name, String value)
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
    public final SelfType setInteger(int position, int value)
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
    public final SelfType setInteger(String name, int value)
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
    public final SelfType setAsciiStream(int position, InputStream value, int length)
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
    public final SelfType setAsciiStream(String name, InputStream value, int length)
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
    public final SelfType setBigDecimal(int position, BigDecimal value)
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
    public final SelfType setBigDecimal(String name, BigDecimal value)
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
    public final SelfType setBinaryStream(int position, InputStream value, int length)
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
    public final SelfType setBinaryStream(String name, InputStream value, int length)
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
    public final SelfType setBlob(int position, Blob value)
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
    public final SelfType setBlob(String name, Blob value)
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
    public final SelfType setBoolean(int position, boolean value)
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
    public final SelfType setBoolean(String name, boolean value)
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
    public final SelfType setByte(int position, byte value)
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
    public final SelfType setByte(String name, byte value)
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
    public final SelfType setBytes(int position, byte[] value)
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
    public final SelfType setBytes(String name, byte[] value)
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
    public final SelfType setCharacterStream(int position, Reader value, int length)
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
    public final SelfType setCharacterStream(String name, Reader value, int length)
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
    public final SelfType setClob(int position, Clob value)
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
    public final SelfType setClob(String name, Clob value)
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
    public final SelfType setDate(int position, java.sql.Date value)
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
    public final SelfType setDate(String name, java.sql.Date value)
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
    public final SelfType setDate(int position, java.util.Date value)
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
    public final SelfType setDate(String name, java.util.Date value)
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
    public final SelfType setDouble(int position, Double value)
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
    public final SelfType setDouble(String name, Double value)
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
    public final SelfType setFloat(int position, Float value)
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
    public final SelfType setFloat(String name, Float value)
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
    public final SelfType setLong(int position, long value)
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
    public final SelfType setLong(String name, long value)
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
    public final SelfType setObject(int position, Object value)
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
    public final SelfType setObject(String name, Object value)
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
    public final SelfType setTime(int position, Time value)
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
    public final SelfType setTime(String name, Time value)
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
    public final SelfType setTimestamp(int position, Timestamp value)
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
    public final SelfType setTimestamp(String name, Timestamp value)
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
    public final SelfType setUrl(int position, URL value)
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
    public final SelfType setUrl(String name, URL value)
    {
        return setArgument(name, new URLArgument(value));
    }

    protected <Result> Result internalExecute(final QueryPreperator prep,
                                              final QueryResultMunger<Result> munger,
                                              final QueryPostMungeCleanup cleanup)
    {
        final ReWrittenStatement rewritten = rewriter.rewrite(sql, getParameters());
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
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
                rewritten.bind(getParameters(), stmt);
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

            try
            {
                stmt.execute();
            }
            catch (SQLException e)
            {
                throw new UnableToExecuteStatementException(e);
            }

            try
            {
                final Pair<Result, ResultSet> r = munger.munge(stmt);
                rs = r.getSecond();
                return r.getFirst();
            }
            catch (SQLException e)
            {
                throw new ResultSetException("Exception thrown while attempting to traverse the result set", e);
            }
        }
        finally
        {
            cleanup.cleanup(this, stmt, rs);
        }
    }
}

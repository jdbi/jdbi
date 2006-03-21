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
import java.util.Map;

public abstract class SQLStatement<SelfType extends SQLStatement<SelfType>>
{
    private final Parameters params;
    private final Connection connection;
    private final String sql;
    private final StatementRewriter rewriter;
    private final PreparedStatementCache preparedStatementCache;

    SQLStatement(Parameters params,
                 StatementRewriter rewriter,
                 Connection conn,
                 PreparedStatementCache preparedStatementCache,
                 String sql)
    {
        assert(verifyOurNastyDowncastIsOkay());
        this.preparedStatementCache = preparedStatementCache;
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

    protected PreparedStatementCache getPreparedStatementCache()
    {
        return preparedStatementCache;
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
     * @param position position to bindBinaryStream this argument, starting at 0
     * @param argument exotic argument factory
     * @return the same Query instance
     */
    @SuppressWarnings("unchecked")
    public SelfType bind(int position, Argument argument)
    {
        params.addPositional(position, argument);
        return (SelfType) this;
    }

    /**
     * Used if you need to have some exotic parameter bound.
     *
     * @param name     name to bindBinaryStream this argument
     * @param argument exotic argument factory
     * @return the same Query instance
     */
    @SuppressWarnings("unchecked")
    public SelfType bind(String name, Argument argument)
    {
        params.addNamed(name, argument);
        return (SelfType) this;
    }

    @SuppressWarnings("unchecked")
    public SelfType bindFromProperties(Object o)
    {
        params.addLazyNamedArguments(new BeanPropertyArguments(o));
        return (SelfType) this;
    }

    @SuppressWarnings("unchecked")
    public SelfType bindFromMap(Map<String, ? extends Object> args)
    {
        params.addLazyNamedArguments(new MapArguments(args));
        return (SelfType) this;
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, String value)
    {
        return bind(position, new StringArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, String value)
    {
        return bind(name, new StringArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, int value)
    {
        return bind(position, new IntegerArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, int value)
    {
        return bind(name, new IntegerArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @param length   how long is the stream being bound?
     * @return the same Query instance
     */
    public final SelfType bindASCIIStream(int position, InputStream value, int length)
    {
        return bind(position, new InputStreamArgument(value, length, true));
    }

    /**
     * Bind an argument by name
     *
     * @param name   token name to bindBinaryStream the paramater to
     * @param value  to bindBinaryStream
     * @param length bytes to read from value
     * @return the same Query instance
     */
    public final SelfType bindASCIIStream(String name, InputStream value, int length)
    {
        return bind(name, new InputStreamArgument(value, length, true));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, BigDecimal value)
    {
        return bind(position, new BigDecimalArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, BigDecimal value)
    {
        return bind(name, new BigDecimalArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bindBinaryStream(int position, InputStream value, int length)
    {
        return bind(position, new InputStreamArgument(value, length, false));
    }

    /**
     * Bind an argument by name
     *
     * @param name   token name to bindBinaryStream the paramater to
     * @param value  to bindBinaryStream
     * @param length bytes to read from value
     * @return the same Query instance
     */
    public final SelfType bindBinaryStream(String name, InputStream value, int length)
    {
        return bind(name, new InputStreamArgument(value, length, false));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, Blob value)
    {
        return bind(position, new BlobArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, Blob value)
    {
        return bind(name, new BlobArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, boolean value)
    {
        return bind(position, new BooleanArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, boolean value)
    {
        return bind(name, new BooleanArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, byte value)
    {
        return bind(position, new ByteArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, byte value)
    {
        return bind(name, new ByteArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, byte[] value)
    {
        return bind(position, new ByteArrayArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, byte[] value)
    {
        return bind(name, new ByteArrayArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @param length   number of characters to read
     * @return the same Query instance
     */
    public final SelfType bind(int position, Reader value, int length)
    {
        return bind(position, new CharacterStreamArgument(value, length));
    }

    /**
     * Bind an argument by name
     *
     * @param name   token name to bindBinaryStream the paramater to
     * @param value  to bindBinaryStream
     * @param length number of characters to read
     * @return the same Query instance
     */
    public final SelfType bind(String name, Reader value, int length)
    {
        return bind(name, new CharacterStreamArgument(value, length));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, Clob value)
    {
        return bind(position, new ClobArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, Clob value)
    {
        return bind(name, new ClobArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, java.sql.Date value)
    {
        return bind(position, new SqlDateArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, java.sql.Date value)
    {
        return bind(name, new SqlDateArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, java.util.Date value)
    {
        return bind(position, new JavaDateArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, java.util.Date value)
    {
        return bind(name, new JavaDateArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, Double value)
    {
        return bind(position, new DoubleArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, Double value)
    {
        return bind(name, new DoubleArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, Float value)
    {
        return bind(position, new FloatArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, Float value)
    {
        return bind(name, new FloatArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, long value)
    {
        return bind(position, new LongArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, long value)
    {
        return bind(name, new LongArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, Object value)
    {
        return bind(position, new ObjectArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, Object value)
    {
        return bind(name, new ObjectArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, Time value)
    {
        return bind(position, new TimeArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, Time value)
    {
        return bind(name, new TimeArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, Timestamp value)
    {
        return bind(position, new TimestampArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, Timestamp value)
    {
        return bind(name, new TimestampArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bindBinaryStream the paramater at, starting at 0
     * @param value    to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(int position, URL value)
    {
        return bind(position, new URLArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bindBinaryStream the paramater to
     * @param value to bindBinaryStream
     * @return the same Query instance
     */
    public final SelfType bind(String name, URL value)
    {
        return bind(name, new URLArgument(value));
    }

    protected <Result> Result internalExecute(final QueryPreperator prep,
                                              final QueryResultMunger<Result> munger,
                                              final QueryPostMungeCleanup cleanup)
    {
        final ReWrittenStatement rewritten = rewriter.rewrite(sql, getParameters());
        final PreparedStatement stmt;
        ResultSet rs = null;
        try
        {
            try
            {
                stmt = preparedStatementCache.locate(rewritten.getSql());
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
                throw new UnableToExecuteStatementException("Unable to bindBinaryStream parameters to query", e);
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
            cleanup.cleanup(this, null, rs);
        }
    }
}

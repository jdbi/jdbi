package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.UnableToCloseResourceException;
import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.tweak.Argument;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * 
 */
public class SQLStatement
{
    private final Connection connection;
    private final String sql;
    private Parameters params;

    SQLStatement(Connection connection, String sql)
    {
        this.params = new Parameters();
        this.connection = connection;
        this.sql = sql;
    }

    public int execute()
    {
        final PreparedStatement stmt;
        try
        {
            stmt = connection.prepareStatement(sql);
        }
        catch (SQLException e)
        {
            throw new UnableToCreateStatementException(e);
        }
        params.apply(stmt);
        try
        {
            int count = stmt.executeUpdate();
            try
            {
                stmt.close();
            }
            catch (SQLException e)
            {
                throw new UnableToCloseResourceException("Unable to close statement", e);
            }
            return count;
        }
        catch (SQLException e)
        {
            String msg = String.format("Unable to execute statement [%s]", sql);
            try
            {
                stmt.close();
            }
            catch (SQLException e1)
            {
                msg = String.format("%s and unable to close the statetement [%s]", msg, e1.getMessage());
            }
            throw new UnableToExecuteStatementException(msg, e);
        }
    }

    /* position param stuff */

    /**
     * Used if you need to have some exotic parameter bound.
     *
     * @param position position to bind this argument, starting at 0
     * @param argument exotic argument factory
     * @return the same Query instance
     */
    public SQLStatement setArgument(int position, Argument argument)
    {
        params.addPositional(position, argument);
        return this;
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setString(int position, String value)
    {
        return setArgument(position, new StringArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setInteger(int position, int value)
    {
        return setArgument(position, new IntegerArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @param length   how long is the stream being bound?
     * @return the same Query instance
     */
    public SQLStatement setAsciiStream(int position, InputStream value, int length)
    {
        return setArgument(position, new InputStreamArgument(value, length, true));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setBigDecimal(int position, BigDecimal value)
    {
        return setArgument(position, new BigDecimalArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setBinaryStream(int position, InputStream value, int length)
    {
        return setArgument(position, new InputStreamArgument(value, length, false));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setBlob(int position, Blob value)
    {
        return setArgument(position, new BlobArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setBoolean(int position, boolean value)
    {
        return setArgument(position, new BooleanArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setByte(int position, byte value)
    {
        return setArgument(position, new ByteArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setBytes(int position, byte[] value)
    {
        return setArgument(position, new ByteArrayArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setCharacterStream(int position, Reader value, int length)
    {
        return setArgument(position, new CharacterStreamArgument(value, length));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setClob(int position, Clob value)
    {
        return setArgument(position, new ClobArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setDate(int position, java.sql.Date value)
    {
        return setArgument(position, new SqlDateArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setDate(int position, java.util.Date value)
    {
        return setArgument(position, new JavaDateArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setDouble(int position, Double value)
    {
        return setArgument(position, new DoubleArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setFloat(int position, Float value)
    {
        return setArgument(position, new FloatArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setLongg(int position, long value)
    {
        return setArgument(position, new LongArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setObject(int position, Object value)
    {
        return setArgument(position, new ObjectArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setTime(int position, Time value)
    {
        return setArgument(position, new TimeArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setTimestamp(int position, Timestamp value)
    {
        return setArgument(position, new TimestampArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the paramater at, starting at 0
     * @param value    to bind
     * @return the same Query instance
     */
    public SQLStatement setUrl(int position, URL value)
    {
        return setArgument(position, new URLArgument(value));
    }


}

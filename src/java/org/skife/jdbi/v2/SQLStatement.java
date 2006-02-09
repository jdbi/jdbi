package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.UnableToCloseResourceException;
import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 
 */
public class SQLStatement
{
    private final Connection connection;
    private final String sql;

    SQLStatement(Connection connection, String sql)
    {
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
}

package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.tweak.StatementRewriter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group of non-prepared statements to be sent to the RDMBS in one "request"
 */
public class Batch
{
    private List<String> parts = new ArrayList<String>();
    private final StatementRewriter rewriter;
    private final Connection connection;

    Batch(StatementRewriter rewriter, Connection connection)
    {
        this.rewriter = rewriter;
        this.connection = connection;
    }

    /**
     * Add a statement to the batch
     *
     * @param sql SQL to be added to the batch, possibly a named statement
     * @return the same Batch statement
     */
    public Batch add(String sql)
    {
        parts.add(sql);
        return this;
    }

    /**
     * Execute all the queued up statements
     *
     * @return an array of integers representing the return values from each statement's execution
     */
    public int[] execute()
    {
        // short circuit empty batch
        if (parts.size() == 0) return new int[] {};

        Parameters empty = new Parameters();
        Statement stmt = null;
        try
        {
            try
            {
                stmt = connection.createStatement();
            }
            catch (SQLException e)
            {
                throw new UnableToCreateStatementException(e);
            }

            try
            {
                for (String part : parts)
                {
                    stmt.addBatch( rewriter.rewrite(part, empty).getSql());
                }
            }
            catch (SQLException e)
            {
                throw new UnableToExecuteStatementException("Unable to configure JDBC statement", e);
            }

            try
            {
                return stmt.executeBatch();
            }
            catch (SQLException e)
            {
                throw new UnableToExecuteStatementException(e);
            }
        }
        finally
        {
            QueryPostMungeCleanup.CLOSE_RESOURCES_QUIETLY.cleanup(null, stmt, null);
        }

    }

}

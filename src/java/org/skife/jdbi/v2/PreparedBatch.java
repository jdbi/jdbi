package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.tweak.ReWrittenStatement;
import org.skife.jdbi.v2.tweak.StatementRewriter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a prepared batch statement. That is, a sql statement compiled as a prepared
 * statement, and then executed multiple times in a single batch. This is, generally,
 * a very efficient way to execute large numbers of the same statement where
 * the statement only varies by the arguments bound to it.
 */
public class PreparedBatch
{
    private List<PreparedBatchPart> parts = new ArrayList<PreparedBatchPart>();
    private final StatementRewriter rewriter;
    private final Connection connection;
    private final PreparedStatementCache preparedStatementCache;
    private final String sql;

    PreparedBatch(StatementRewriter rewriter, Connection connection, PreparedStatementCache preparedStatementCache, String sql)
    {
        this.rewriter = rewriter;
        this.connection = connection;
        this.preparedStatementCache = preparedStatementCache;
        this.sql = sql;
    }

    /**
     * Execute the batch
     * @return the number of rows modified or inserted per batch part.
     */
    public int[] execute()
    {
        // short circuit empty batch
        if (parts.size() == 0) return new int[] {};

        PreparedBatchPart current = parts.get(0);
        final ReWrittenStatement rewritten = rewriter.rewrite(sql, current.getParameters());
        PreparedStatement stmt = null;
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
                for (PreparedBatchPart part : parts)
                {
                    rewritten.bind(part.getParameters(), stmt);
                    stmt.addBatch();
                }
            }
            catch (SQLException e)
            {
                throw new UnableToExecuteStatementException("Unable to configure JDBC statement to 1", e);
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

    /**
     * Add a statement (part) to this batch. You'll need to bindBinaryStream any arguments to the
     * part.
     *
     * @return A part which can be used to bindBinaryStream parts to the statement
     */
    public PreparedBatchPart add()
    {
        PreparedBatchPart part = new PreparedBatchPart(this, rewriter, connection, preparedStatementCache, sql);
        parts.add(part);
        return part;
    }

    public PreparedBatchPart add(Object bean)
    {
        PreparedBatchPart part = new PreparedBatchPart(this, rewriter, connection, preparedStatementCache,sql);
        parts.add(part);
        part.bindFromProperties(bean);
        return part;
    }

    public PreparedBatchPart add(Map<String, ? extends Object> args)
    {
        PreparedBatchPart part = new PreparedBatchPart(this, rewriter, connection, preparedStatementCache,sql);
        parts.add(part);
        part.bindFromMap(args);
        return part;
    }
}

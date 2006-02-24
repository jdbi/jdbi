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

public class PreparedBatch
{
    private List<PreparedBatchPart> parts = new ArrayList<PreparedBatchPart>();
    private final StatementRewriter rewriter;
    private final Connection connection;
    private final String sql;

    PreparedBatch(StatementRewriter rewriter, Connection connection, String sql)
    {
        this.rewriter = rewriter;
        this.connection = connection;
        this.sql = sql;
    }

    public int[] execute()
    {
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

    public PreparedBatchPart add()
    {
        PreparedBatchPart part = new PreparedBatchPart(this, rewriter, connection, sql);
        parts.add(part);
        return part;
    }
}

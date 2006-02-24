package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.ReWrittenStatement;
import org.skife.jdbi.v2.tweak.StatementRewriter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PreparedBatch extends SQLStatement<PreparedBatch>
{
    private PreparedBatchPart current;
    private List<PreparedBatchPart> parts = new ArrayList<PreparedBatchPart>();

    PreparedBatch(StatementRewriter rewriter, Connection connection, String sql)
    {
        super(null, rewriter, connection, sql);
        current = new PreparedBatchPart(rewriter, connection, sql);
    }

    public int[] execute()
    {
        parts.add(current);
        final ReWrittenStatement rewritten = getRewriter().rewrite(getSql(), current.getParameters());
        PreparedStatement stmt = null;
        try
        {
            try
            {
                stmt = getConnection().prepareStatement(rewritten.getSql());
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
            QueryPostMungeCleanup.CLOSE_RESOURCES_QUIETLY.cleanup(this, stmt, null);
        }
    }

    public PreparedBatch addAnother()
    {
        parts.add(current);
        current = new PreparedBatchPart(getRewriter(), getConnection(), getSql());
        return this;
    }

    public PreparedBatch setArgument(int position, Argument argument)
    {
        current.setArgument(position, argument);
        return this;
    }

    public PreparedBatch setArgument(String name, Argument argument)
    {
        current.setArgument(name, argument);
        return this;
    }
}

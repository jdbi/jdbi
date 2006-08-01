package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.StatementLocator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 
 */
public class UpdateStatement extends SQLStatement<UpdateStatement>
{
    UpdateStatement(Connection connection, StatementLocator locator, StatementRewriter statementRewriter, PreparedStatementCache cache, String sql)
    {
        super(new Parameters(), locator, statementRewriter, connection, cache, sql);
    }

    public int execute()
    {
        return this.internalExecute(QueryPreperator.NO_OP, new QueryResultMunger<Integer>()
        {
            public Pair<Integer, ResultSet> munge(Statement results) throws SQLException
            {
                return new Pair<Integer, ResultSet>(results.getUpdateCount(), null);
            }
        }, QueryPostMungeCleanup.CLOSE_RESOURCES_QUIETLY);
    }
}

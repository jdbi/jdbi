package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementBuilder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Used for INSERT, UPDATE, and DELETE statements
 */
public class Update extends SQLStatement<Update>
{
    Update(Connection connection, StatementLocator locator, StatementRewriter statementRewriter, StatementBuilder cache, String sql)
    {
        super(new Binding(), locator, statementRewriter, connection, cache, sql);
    }

    /**
     * Execute the statement
     * @return the number of rows modified
     */
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

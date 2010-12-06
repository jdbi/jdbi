package org.skife.jdbi.v2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

abstract class QueryResultSetMunger<T>
        implements QueryResultMunger<T>
{
    public final T munge(Statement results)
            throws SQLException
    {
        ResultSet rs = results.getResultSet();
        try {
            return munge(rs);
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) {
                    // ignore
                }
            }
        }
    }

    protected abstract T munge(ResultSet rs)
            throws SQLException;
}

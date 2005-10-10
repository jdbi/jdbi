package org.skife.jdbi.unstable.metadata;

import org.skife.jdbi.DBIException;

import java.sql.SQLException;

abstract class Helper
{
    static Object sql(Helper helper)
    {
        try
        {
            return helper.fetch();
        }
        catch (SQLException e)
        {
            throw new DBIException(e);
        }
    }

    abstract Object fetch() throws SQLException;

}

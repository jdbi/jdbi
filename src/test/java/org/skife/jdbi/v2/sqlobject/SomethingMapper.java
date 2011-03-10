package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;


public class SomethingMapper implements ResultSetMapper<Something>
{
    public Something map(int index, ResultSet r, StatementContext ctx) throws SQLException
    {
        return new Something(r.getInt("id"), r.getString("name"));
    }
}


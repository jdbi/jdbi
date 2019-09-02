package org.jdbi.v3.core.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;

public class JoinCollector<R> implements RowMapper.Specialized<R> {
    @Override
    public RowMapper<R> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
}

package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.SQLStatement;

import java.sql.SQLException;

public interface SQLStatementCustomizer
{
    void apply(SQLStatement q) throws SQLException;
}

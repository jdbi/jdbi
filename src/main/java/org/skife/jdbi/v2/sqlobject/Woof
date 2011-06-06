package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.SQLStatement;

import java.sql.SQLException;

/**
 * Used with {@link org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory} to
 * customize sql statements via annotations
 */
public interface SqlStatementCustomizer
{
    /**
     * Invoked to customize the sql statement
     * @param q the statement being customized
     * @throws SQLException will abort statement creation
     */
    void apply(SQLStatement q) throws SQLException;
}

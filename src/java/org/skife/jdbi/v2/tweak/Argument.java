package org.skife.jdbi.v2.tweak;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Represents an argument to a prepared statement. It will be called right before the
 * statement is executed to bind the parameter.
 */
public interface Argument
{
    /**
     * Callback method invoked right before statement execution.
     *
     * @param position the position to which the argument should be bound, using the
     *                 stupid JDBC "start at 1" bit
     * @param statement the prepared statement the argument is to be bound to
     * @throws SQLException if anything goes wrong
     */
    void apply(final int position, PreparedStatement statement) throws SQLException;
}

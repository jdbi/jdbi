package org.skife.jdbi.v2;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class BigDecimalArgument implements Argument
{
    private final BigDecimal value;

    BigDecimalArgument(BigDecimal value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setBigDecimal(position, value);
    }
}

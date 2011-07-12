package org.skife.jdbi.v2.util;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BigDecimalMapper extends TypedMapper<BigDecimal>
{
    public BigDecimalMapper()
    {
        super();
    }

    public BigDecimalMapper(int index)
    {
        super(index);
    }

    public BigDecimalMapper(String name)
    {
        super(name);
    }

    @Override
    protected BigDecimal extractByName(ResultSet r, String name) throws SQLException
    {
        return r.getBigDecimal(name);
    }

    @Override
    protected BigDecimal extractByIndex(ResultSet r, int index) throws SQLException
    {
        return r.getBigDecimal(index);
    }

    public static final BigDecimalMapper FIRST = new BigDecimalMapper();
}

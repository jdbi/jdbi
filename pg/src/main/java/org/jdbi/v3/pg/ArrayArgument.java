package org.jdbi.v3.pg;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jdbi.v3.StatementContext;
import org.jdbi.v3.tweak.Argument;

public class ArrayArgument implements Argument {

    private final String elementType;
    private final Object[] array;

    public ArrayArgument(String elementType, Object... array) {
        this.elementType = elementType;
        this.array = array;
    }

    @Override
    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
        Array sqlArray = statement.getConnection().createArrayOf(elementType, array);
        statement.setArray(position, sqlArray);
    }
}

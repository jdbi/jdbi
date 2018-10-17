package org.jdbi.v3.core.argument;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ObjectToStringBinder<T> implements StatementBinder<T> {
    private final StatementBinder<String> stringSetter;

    public ObjectToStringBinder(StatementBinder<String> stringSetter) {
        this.stringSetter = stringSetter;
    }

    @Override
    public void bind(PreparedStatement p, int index, T value) throws SQLException {
        stringSetter.bind(p, index, String.valueOf(value));
    }
}

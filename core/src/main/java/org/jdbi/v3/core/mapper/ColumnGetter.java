package org.jdbi.v3.core.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
interface ColumnGetter<T> {
    T get(ResultSet rs, int i) throws SQLException;
}

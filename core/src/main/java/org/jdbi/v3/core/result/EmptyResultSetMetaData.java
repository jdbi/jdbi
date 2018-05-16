package org.jdbi.v3.core.result;

import java.sql.ResultSetMetaData;

public class EmptyResultSetMetaData implements ResultSetMetaData {
    public static final EmptyResultSetMetaData INSTANCE = new EmptyResultSetMetaData();

    private EmptyResultSetMetaData() {}

    @Override
    public int getColumnCount() {
        return 0;
    }

    @Override
    public boolean isAutoIncrement(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCaseSensitive(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSearchable(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCurrency(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int isNullable(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSigned(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getColumnDisplaySize(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getColumnLabel(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getColumnName(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSchemaName(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPrecision(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getScale(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTableName(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCatalogName(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getColumnType(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getColumnTypeName(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadOnly(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWritable(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDefinitelyWritable(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getColumnClassName(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        throw new UnsupportedOperationException();
    }
}

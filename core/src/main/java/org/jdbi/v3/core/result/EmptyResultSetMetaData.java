/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.core.result;

import java.sql.ResultSetMetaData;

class EmptyResultSetMetaData implements ResultSetMetaData {
    static final EmptyResultSetMetaData INSTANCE = new EmptyResultSetMetaData();

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

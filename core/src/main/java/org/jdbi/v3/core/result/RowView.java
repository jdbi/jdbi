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

import java.lang.reflect.Type;
import java.util.List;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.meta.Alpha;
import org.jdbi.v3.meta.Beta;

/**
 * A RowView is an accessor for {@code ResultSet} that uses
 * {@code RowMapper} or {@code ColumnMapper} to extract values.
 * It is not valid outside the scope of the method that receives it.
 */
// TODO v4: should be interface, but that's a breaking change
public abstract class RowView {
    /**
     * Use a row mapper to extract a type from the current ResultSet row.
     * @param <T> the type to map
     * @param rowType the Class of the type
     * @return the materialized T
     */
    public <T> T getRow(Class<T> rowType) {
        return rowType.cast(getRow((Type) rowType));
    }

    /**
     * Use a prefixed row mapper to extract a type from the current ResultSet row.
     * The mapper must declare the given column name prefix, see {@link #getRow(Type, String)}.
     * @param <T> the type to map
     * @param rowType the Class of the type
     * @param prefix the column name prefix the mapper must declare, never null
     * @return the materialized T
     */
    @Alpha
    public <T> T getRow(Class<T> rowType, String prefix) {
        return rowType.cast(getRow((Type) rowType, prefix));
    }

    /**
     * Use a row mapper to extract a type from the current ResultSet row.
     * @param <T> the type to map
     * @param rowType the GenericType of the type
     * @return the materialized T
     */
    @SuppressWarnings("unchecked")
    public <T> T getRow(GenericType<T> rowType) {
        return (T) getRow(rowType.getType());
    }

    /**
     * Use a prefixed row mapper to extract a type from the current ResultSet row.
     * The mapper must declare the given column name prefix, see {@link #getRow(Type, String)}.
     * @param <T> the type to map
     * @param rowType the GenericType of the type
     * @param prefix the column name prefix the mapper must declare, never null
     * @return the materialized T
     */
    @Alpha
    @SuppressWarnings("unchecked")
    public <T> T getRow(GenericType<T> rowType, String prefix) {
        return (T) getRow(rowType.getType(), prefix);
    }

    /**
     * Use a row mapper to extract a type from the current ResultSet row.
     * @param type the type to map
     * @return the materialized object
     */
    public abstract Object getRow(Type type);

    /**
     * Use a prefixed row mapper to extract a type from the current ResultSet row.
     * <p>
     * Only mappers that implement {@link org.jdbi.v3.core.mapper.PrefixedRowMapper} and declare a
     * prefix equal to the given prefix take part in the lookup. This makes it possible to register
     * multiple mappers for the same type with different column name prefixes, for example when a
     * query joins the same table twice, and select between them per call. A mapper that does not
     * declare the given prefix never matches; the lookup fails rather than fall back to an
     * unprefixed mapper for the type. To look up a mapper by type alone, use {@link #getRow(Type)}.
     *
     * @param type the type to map
     * @param prefix the column name prefix the mapper must declare, never null
     * @return the materialized object
     * @throws org.jdbi.v3.core.mapper.NoSuchMapperException if no registered row mapper maps the given type with the given prefix
     */
    @Alpha
    public Object getRow(Type type, String prefix) {
        throw new UnsupportedOperationException("getRow by prefix is not supported by " + getClass().getName());
    }

    /**
     * Returns the labels of the columns of the result set, in column order, as {@link java.sql.ResultSetMetaData#getColumnLabel(int)}
     * returns them.
     * @return the column labels
     */
    @Beta
    public List<String> getColumnNames() {
        throw new UnsupportedOperationException("getColumnNames is not supported by " + getClass().getName());
    }

    /**
     * Maps the current row with the given row mapper. The mapper is {@link RowMapper#specialize specialized}
     * once per result set, keyed by mapper instance, so pass the same instance for every row.
     * @param <T> the type to map
     * @param mapper the row mapper
     * @return the materialized T
     */
    @Beta
    public <T> T getRow(RowMapper<T> mapper) {
        throw new UnsupportedOperationException("getRow with a mapper is not supported by " + getClass().getName());
    }

    /**
     * Returns the value of a column as the JDBC driver returns it from {@link java.sql.ResultSet#getObject(String)},
     * without a column mapper. Use this to read a value whose type does not matter, such as a key that identifies a row.
     * @param column the column name
     * @return the column value, or null for SQL NULL
     */
    @Beta
    public Object getColumn(String column) {
        throw new UnsupportedOperationException("getColumn without a type is not supported by " + getClass().getName());
    }

    /**
     * Returns the value of a column as the JDBC driver returns it from {@link java.sql.ResultSet#getObject(int)},
     * without a column mapper. Use this to read a value whose type does not matter, such as a key that identifies a row.
     * @param column the column index
     * @return the column value, or null for SQL NULL
     */
    @Beta
    public Object getColumn(int column) {
        throw new UnsupportedOperationException("getColumn without a type is not supported by " + getClass().getName());
    }

    /**
     * Use a column mapper to extract a type from the current ResultSet row.
     * @param <T> the type to map
     * @param column the column name
     * @param type the Class of the type
     * @return the materialized T
     */
    public <T> T getColumn(String column, Class<T> type) {
        return type.cast(getColumn(column, (Type) type));
    }

    /**
     * Use a column mapper to extract a type from the current ResultSet row.
     * @param <T> the type to map
     * @param column the column index
     * @param type the Class of the type
     * @return the materialized T
     */
    public <T> T getColumn(int column, Class<T> type) {
        return type.cast(getColumn(column, (Type) type));
    }

    /**
     * Use a column mapper to extract a type from the current ResultSet row.
     * @param <T> the type to map
     * @param column the column name
     * @param type the GenericType of the type
     * @return the materialized T
     */
    @SuppressWarnings("unchecked")
    public <T> T getColumn(String column, GenericType<T> type) {
        return (T) getColumn(column, type.getType());
    }

    /**
     * Use a qualified column mapper to extract a type from the current ResultSet row.
     * @param <T> the type to map
     * @param column the column index
     * @param type the QualifiedType of the type
     * @return the materialized T
     */
    public abstract <T> T getColumn(int column, QualifiedType<T> type);

    /**
     * Use a qualified column mapper to extract a type from the current ResultSet row.
     * @param <T> the type to map
     * @param column the column name
     * @param type the QualifiedType of the type
     * @return the materialized T
     */
    public abstract <T> T getColumn(String column, QualifiedType<T> type);

    /**
     * Use a column mapper to extract a type from the current ResultSet row.
     * @param <T> the type to map
     * @param column the column index
     * @param type the GenericType of the type
     * @return the materialized T
     */
    @SuppressWarnings("unchecked")
    public <T> T getColumn(int column, GenericType<T> type) {
        return (T) getColumn(column, type.getType());
    }

    /**
     * Use a column mapper to extract a type from the current ResultSet row.
     * @param column the column name
     * @param type the Type of the type
     * @return the materialized object
     */
    public Object getColumn(String column, Type type) {
        return getColumn(column, QualifiedType.of(type));
    }

    /**
     * Use a column mapper to extract a type from the current ResultSet row.
     * @param column the column name
     * @param type the Class of the type
     * @return the materialized object
     */
    public Object getColumn(int column, Type type) {
        return getColumn(column, QualifiedType.of(type));
    }
}

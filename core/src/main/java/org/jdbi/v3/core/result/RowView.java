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

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.qualifier.QualifiedType;

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
     * Use a row mapper to extract a type from the current ResultSet row.
     * @param type the type to map
     * @return the materialized object
     */
    public abstract Object getRow(Type type);

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

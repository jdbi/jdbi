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
package org.jdbi.v3.core;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.exception.UnableToExecuteStatementException;
import org.jdbi.v3.core.util.GenericType;

/**
 * A RowView is an accessor for {@code ResultSet} that uses
 * {@code RowMapper} or {@code ColumnMapper} to extract values.
 * It is not valid outside the scope of the method that receives it.
 */
public class RowView
{
    private final StatementContext ctx;
    private final ResultSet rs;

    RowView(ResultSet rs, StatementContext ctx)
    {
        this.rs = rs;
        this.ctx = ctx;
    }

    /**
     * Use a row mapper to extract a type from the current ResultSet row.
     */
    public <T> T getRow(Class<T> rowType)
    {
        return rowType.cast(getRow((Type) rowType));
    }

    /**
     * Use a row mapper to extract a type from the current ResultSet row.
     */
    @SuppressWarnings("unchecked")
    public <T> T getRow(GenericType<T> rowType)
    {
        return (T) getRow(rowType.getType());
    }

    /**
     * Use a row mapper to extract a type from the current ResultSet row.
     */
    public Object getRow(Type type)
    {
        try {
            return ctx.findRowMapperFor(type)
                    .orElseThrow(() -> new UnableToExecuteStatementException("No row mapper for " + type, ctx))
                    .map(rs, ctx);
        } catch (SQLException e) {
            throw new UnableToExecuteStatementException(e, ctx);
        }
    }

    /**
     * Use a column mapper to extract a type from the current ResultSet row.
     */
    public <T> T getColumn(String column, Class<T> type)
    {
        return type.cast(getColumn(column, (Type) type));
    }
    /**
     * Use a column mapper to extract a type from the current ResultSet row.
     */
    public <T> T getColumn(int column, Class<T> type)
    {
        return type.cast(getColumn(column, (Type) type));
    }

    /**
     * Use a column mapper to extract a type from the current ResultSet row.
     */
    @SuppressWarnings("unchecked")
    public <T> T getColumn(String column, GenericType<T> type)
    {
        return (T) getColumn(column, type.getType());
    }
    /**
     * Use a column mapper to extract a type from the current ResultSet row.
     */
    @SuppressWarnings("unchecked")
    public <T> T getColumn(int column, GenericType<T> type)
    {
        return (T) getColumn(column, type.getType());
    }

    /**
     * Use a column mapper to extract a type from the current ResultSet row.
     */
    public Object getColumn(String column, Type type)
    {
        try {
            return ctx.findColumnMapperFor(type)
                    .orElseThrow(() -> new UnableToExecuteStatementException("No column mapper for " + type, ctx))
                    .map(rs, column, ctx);
        } catch (SQLException e) {
            throw new UnableToExecuteStatementException(e, ctx);
        }
    }

    /**
     * Use a column mapper to extract a type from the current ResultSet row.
     */
    public Object getColumn(int column, Type type)
    {
        try {
            return ctx.findColumnMapperFor(type)
                    .orElseThrow(() -> new UnableToExecuteStatementException("No column mapper for " + type, ctx))
                    .map(rs, column, ctx);
        } catch (SQLException e) {
            throw new UnableToExecuteStatementException(e, ctx);
        }
    }
}

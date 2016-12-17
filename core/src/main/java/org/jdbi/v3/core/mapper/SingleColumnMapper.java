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
package org.jdbi.v3.core.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;

/**
 * Adapts a {@link ColumnMapper} into a {@link RowMapper} by mapping a single column.
 */
public class SingleColumnMapper<T> implements RowMapper<T> {
    private final RowMapper<T> delegate;

    /**
     * Constructs a row mapper which maps the first column.
     * @param mapper the column mapper to delegate to for mapping.
     */
    public SingleColumnMapper(ColumnMapper<T> mapper) {
        this(mapper, 1);
    }

    /**
     * Constructs a row mapper which maps the given column number.
     * @param mapper the column mapper to delegate to for mapping
     * @param columnNumber the column number (1-based) to map
     */
    public SingleColumnMapper(ColumnMapper<T> mapper, int columnNumber) {
        this.delegate = (r, ctx) -> mapper.map(r, columnNumber, ctx);
    }

    /**
     * Constructs a row mapper which maps the column with the given label.
     * @param mapper the column mapper to delegate to for mapping
     * @param columnLabel the label of the column to map
     */
    public SingleColumnMapper(ColumnMapper<T> mapper, String columnLabel) {
        this.delegate = (r, ctx) -> mapper.map(r, columnLabel, ctx);
    }

    @Override
    public T map(ResultSet r, StatementContext ctx) throws SQLException {
        return delegate.map(r, ctx);
    }
}

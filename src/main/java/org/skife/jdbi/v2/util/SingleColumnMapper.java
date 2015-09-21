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
package org.skife.jdbi.v2.util;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultColumnMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Adapts a {@link ResultColumnMapper} into a {@link ResultSetMapper} by mapping a single column.
 */
public class SingleColumnMapper<T> implements ResultSetMapper<T> {
    private final ResultSetMapper<T> delegate;

    /**
     * Constructs a result set mapper which maps the first column.
     * @param columnMapper the column mapper to delegate to for mapping.
     */
    public SingleColumnMapper(ResultColumnMapper<T> columnMapper) {
        this(columnMapper, 1);
    }

    /**
     * Constructs a result set mapper which maps the given column number.
     * @param columnMapper the column mapper to delegate to for mapping
     * @param columnNumber the column number (1-based) to map
     */
    public SingleColumnMapper(ResultColumnMapper<T> columnMapper, int columnNumber) {
        this.delegate = new ByNumber<>(columnMapper, columnNumber);
    }

    /**
     * Constructs a result set mapper which maps the column with the given label.
     * @param columnMapper the column mapper to delegate to for mapping
     * @param columnLabel the column label to map
     */
    public SingleColumnMapper(ResultColumnMapper<T> columnMapper, String columnLabel) {
        this.delegate = new ByLabel<>(columnMapper, columnLabel);
    }

    @Override
    public T map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return delegate.map(index, r, ctx);
    }

    private static class ByLabel<T> implements ResultSetMapper<T> {
        private final ResultColumnMapper<T> mapper;
        private final String label;

        public ByLabel(ResultColumnMapper<T> mapper, String label) {
            this.mapper = mapper;
            this.label = label;
        }

        @Override
        public T map(int index, ResultSet r, StatementContext ctx) throws SQLException {
            return mapper.mapColumn(r, label, ctx);
        }
    }

    private static class ByNumber<T> implements ResultSetMapper<T> {
        private final ResultColumnMapper<T> mapper;
        private final int number;

        public ByNumber(ResultColumnMapper<T> mapper, int number) {
            this.mapper = mapper;
            this.number = number;
        }

        @Override
        public T map(int index, ResultSet r, StatementContext ctx) throws SQLException {
            return mapper.mapColumn(r, number, ctx);
        }
    }
}

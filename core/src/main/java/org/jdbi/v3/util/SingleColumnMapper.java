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
package org.jdbi.v3.util;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.StatementContext;
import org.jdbi.v3.tweak.ResultColumnMapper;
import org.jdbi.v3.tweak.ResultSetMapper;

/**
 * Adapts a {@link ResultColumnMapper} into a {@link ResultSetMapper} by mapping a single column.
 */
public class SingleColumnMapper<T> implements ResultSetMapper<T> {
    private final ResultColumnMapper<T> mapper;
    private final int number;

    /**
     * Constructs a result set mapper which maps the first column.
     * @param mapper the column mapper to delegate to for mapping.
     */
    public SingleColumnMapper(ResultColumnMapper<T> mapper) {
        this(mapper, 1);
    }

    /**
     * Constructs a result set mapper which maps the given column number.
     * @param mapper the column mapper to delegate to for mapping
     * @param number the column number (1-based) to map
     */
    public SingleColumnMapper(ResultColumnMapper<T> mapper, int number) {
        this.mapper = mapper;
        this.number = number;
    }

    @Override
    public T map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return mapper.mapColumn(r, number, ctx);
    }
}

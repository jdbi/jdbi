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
package org.jdbi.v3.tweak;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.StatementContext;

/**
 * Used with a {@link org.jdbi.v3.Query#map(RowMapper)} call to specify
 * what to do with each row of a result set
 */
@FunctionalInterface
public interface RowMapper<T>
{
    /**
     * Map the row the result set is at when passed in. This method should not cause the result
     * set to advance, allow jDBI to do that, please.
     *
     * @param r the result set being iterated
     * @param ctx the statement context
     * @return the value to return for this row
     * @throws SQLException if anything goes wrong go ahead and let this percolate, jDBI will handle it
     */
    T map(ResultSet r, StatementContext ctx) throws SQLException;
}

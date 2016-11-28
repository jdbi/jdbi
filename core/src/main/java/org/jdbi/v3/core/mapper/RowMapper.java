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

import org.jdbi.v3.core.Query;
import org.jdbi.v3.core.StatementContext;

/**
 * Used with a {@link Query#map(RowMapper)} call to specify
 * what to do with each row of a result set
 */
@FunctionalInterface
public interface RowMapper<T>
{
    /**
     * Map the row the result set is at when passed in. This method should not cause the result
     * set to advance, allow jDBI to do that, please.
     *
     * @param rs the result set being iterated
     * @param ctx the statement context
     * @return the value to return for this row
     * @throws SQLException if anything goes wrong go ahead and let this percolate, jDBI will handle it
     */
    T map(ResultSet rs, StatementContext ctx) throws SQLException;

    /**
     * Returns a specialized row mapper, optimized for the given result set.
     * <p>
     * Before mapping the result set from a SQL statement, JDBI will first call this method to obtain a specialized
     * instance. The returned mapper will then be used for each row in the result set, and discarded.
     * <p>
     * Implementing this method is optional; the default implementation returns {@code this}. Implementors might choose
     * to override this method to improve performance, e.g. by mapping result columns once for the whole result set,
     * and not for every row.
     *
     * @param rs  the result set to specialize over
     * @param ctx the statement context to specialize over
     * @return a row mapper equivalent to this one, possibly specialized.
     * @throws SQLException if anything goes wrong go ahead and let this percolate, jDBI will handle it
     * @see org.jdbi.v3.core.mapper.reflect.BeanMapper for an example of memoization.
     */
    default RowMapper<T> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        return this;
    }
}

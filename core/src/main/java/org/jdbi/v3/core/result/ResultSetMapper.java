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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

import org.jdbi.v3.core.statement.StatementContext;

/**
 * Maps result sets to objects.
 *
 * @param <T> the mapped type
 * @see org.jdbi.v3.core.result.ResultBearing#mapResultSet(ResultSetMapper)
 */
@FunctionalInterface
public interface ResultSetMapper<T> {
    /**
     * Maps the lazily-supplied result set into an object. The result set is not opened (and typically, the statement
     * the result came from is not executed) until {@code resultSetSupplier.get()} is called.
     * <p>
     * Implementors that call {@code resultSetSupplier.get()} must ensure that the statement context is closed, to
     * ensure that database resources are freed:
     * <pre>
     * try {
     *     ResultSet resultSet = resultSetSupplier.get()
     *     // generate and return result from the result set.
     * }
     * finally {
     *     ctx.close();
     * }
     * </pre>
     * <p>
     * Alternatively, implementors may return some intermediate result object (e.g. {@link ResultIterable}) without
     * calling {@code resultSetSupplier.get()}, in which case the burden of closing resources falls to whichever object
     * ultimately does {@code get()} the result set.
     *
     * @param resultSetSupplier supplies a ResultSet.
     * @param ctx               the statement context.
     * @return the mapped result
     * @throws SQLException if anything goes wrong
     */
    T mapResultSet(Supplier<ResultSet> resultSetSupplier, StatementContext ctx) throws SQLException;
}

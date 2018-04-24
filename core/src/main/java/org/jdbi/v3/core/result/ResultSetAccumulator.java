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

import org.jdbi.v3.core.statement.StatementContext;

/**
 * A {@link ResultSetAccumulator} repeatedly combines rows
 * from the given {@code ResultSet} to produce a single
 * result.  {@code jdbi} will advance the {@code ResultSet} between
 * each method invocation, so don't call {@link ResultSet#next()} please.
 */
@FunctionalInterface
public interface ResultSetAccumulator<T> {
    /**
     * Extract a single row from the result set, and combine it with the
     * accumulator to produce a result.
     * @param in the previous object
     * @param rs the ResultSet, please do not advance it
     * @param ctx the statement context
     * @return the accumulated value
     * @throws SQLException in the face of grave danger
     */
    T apply(T in, ResultSet rs, StatementContext ctx) throws SQLException;
}

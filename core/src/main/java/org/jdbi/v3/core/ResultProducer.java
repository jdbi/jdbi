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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A {@code ResultProducer} generates the mapped results of a {@link SqlStatement},
 * potentially wrapped in a container object.
 */
@FunctionalInterface
public interface ResultProducer<T, R>
{
    /**
     * Produce a statement result.
     * @param stmt the prepared statement
     * @param rs the result set to iterate
     * @param ctx the statement context
     * @return an object of the type your caller expects
     * @throws SQLException sadness
     */
    R produce(PreparedStatement stmt, ResultSet rs, StatementContext ctx) throws SQLException;
}

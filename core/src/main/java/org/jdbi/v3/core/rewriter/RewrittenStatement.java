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
package org.jdbi.v3.core.rewriter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jdbi.v3.core.Binding;
import org.jdbi.v3.core.StatementContext;

/**
 * Return value from {@link StatementRewriter#rewrite(String, Binding, StatementContext)} calls
 */
public interface RewrittenStatement
{
    /**
     * Binds a set of parameters to a prepared statement. The
     * statement will have been constructed from this RewrittenStatement's
     * getSql() return result
     * @param params the parameters to bind to the statement
     * @param statement the prepared statement to bind the parameters to.
     * @throws SQLException if anything goes wrong
     */
    void bind(Binding params, PreparedStatement statement) throws SQLException;

    /**
     * @return the SQL in valid (rewritten) form to be used to prepare a statement
     */
    String getSql();
}

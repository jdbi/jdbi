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
package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.Binding;
import org.skife.jdbi.v2.StatementContext;

/**
 * Use to provide arbitrary statement rewriting.
 */
public interface StatementRewriter
{
    /**
     * Munge up the SQL as desired. Responsible for figuring out how to bind any
     * arguments in to the resultant prepared statement.
     *
     * @param sql The SQL to rewrite
     * @param params contains the arguments which have been bound to this statement.
     * @param ctx The statement context for the statement being executed
     * @return something which can provide the actual SQL to prepare a statement from
     *         and which can bind the correct arguments to that prepared statement
     */
    RewrittenStatement rewrite(String sql, Binding params, StatementContext ctx);
}

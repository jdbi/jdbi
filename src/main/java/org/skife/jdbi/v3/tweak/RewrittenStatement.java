/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
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
package org.skife.jdbi.v3.tweak;

import org.skife.jdbi.v3.Binding;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Return value from {@link StatementRewriter#rewrite(String, org.skife.jdbi.v3.Binding, org.skife.jdbi.v3.StatementContext)} calls
 */
public interface RewrittenStatement
{
    /**
     * Called to bind a set of parameters to a prepared statement. The
     * statement will have been constructed from this RewrittenStatement's
     * getSql() return result
     * @param params
     * @param statement
     * @throws SQLException
     */
    public void bind(Binding params, PreparedStatement statement) throws SQLException;

    /**
     * Obtain the SQL in valid (rewritten) form to be used to prepare a statement
     */
    public String getSql();
}

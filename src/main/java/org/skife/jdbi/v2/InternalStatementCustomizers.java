/*
 * Copyright 2004 - 2011 Brian McCallister
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

package org.skife.jdbi.v2;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.skife.jdbi.v2.tweak.BaseStatementCustomizer;
import org.skife.jdbi.v2.tweak.StatementCustomizer;

class InternalStatementCustomizers
{
    private InternalStatementCustomizers()
    {
    }

    /**
     * Hint to the statement, that we want only a single row. Used by {@link Query#first()} to limit the number
     * of rows returned by the database.
     */
    static final StatementCustomizer MAX_ROW_ONE = new BaseStatementCustomizer()
    {
        public void beforeExecution(final PreparedStatement stmt, final StatementContext ctx) throws SQLException
        {
            stmt.setMaxRows(1);
        }
    };
}

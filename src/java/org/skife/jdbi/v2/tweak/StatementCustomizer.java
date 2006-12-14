/*
 * Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.StatementContext;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Used t customize a prepared statement.
 */
public interface StatementCustomizer
{
    /**
     * Make the changes you need to inside this method.
     *
     * @param stmt Prepared statement being customized
     * @param ctx Statement context associated with the statement being customized
     * @throws SQLException go ahead and percolate it for jDBI to handle
     */
    public void customize(PreparedStatement stmt, StatementContext ctx) throws SQLException;
}

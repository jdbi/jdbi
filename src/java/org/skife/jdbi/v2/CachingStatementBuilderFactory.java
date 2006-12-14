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

package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementBuilderFactory;
import org.skife.jdbi.v2.tweak.StatementBuilder;

import java.sql.Connection;

/**
 * The default StatementBuilderFactory. Provides StatementBuilder instances
 * which cache all prepared statements for a given handle instance.
 */
public class CachingStatementBuilderFactory implements StatementBuilderFactory
{
    /**
     * Return a new, or cached, prepared statement
     */
    public StatementBuilder createStatementBuilder(Connection conn) {
        return new PreparedStatementCache(conn);
    }
}

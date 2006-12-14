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

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Used to convert translated SQL into a prepared statement. The default implementation
 * created by {@link org.skife.jdbi.v2.CachingStatementBuilderFactory} caches all prepared
 * statements created against a given handle.
 * @see StatementBuilderFactory
 */
public interface StatementBuilder
{
    /**
     * Called each time a prepared statement needs to be created
     * @param sql the translated SQL which should be prepared
     */
    PreparedStatement create(String sql) throws SQLException;

    /**
     * Called when the handle this StatementBuilder is attached to is closed.
     */
    void close();
}

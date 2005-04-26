/* Copyright 2004-2005 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.unstable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Used to map a single row to an instance of Map. Yes, it must be a Map. It can be anything else as well.
 * Implementations must not modify the state of the resultset.
 */
public interface RowMapper extends Unstable
{
    /**
     * The column names requested by the query (using alias instead of name if specified)
     * and a result set on the row to map.
     * <p>
     * Implementations must not modify the state of the resultset.
     */
    public Map map(String[] column_names, ResultSet row) throws SQLException;
}

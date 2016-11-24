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
package org.jdbi.v3.postgres;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.mapper.ColumnMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * A column mapper which maps Postgres' hstore type to Java's {@link Map}.
 */
public class HStoreColumnMapper implements ColumnMapper<Map<String, String>> {

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        return (Map<String, String>) r.getObject(columnNumber);
    }
}

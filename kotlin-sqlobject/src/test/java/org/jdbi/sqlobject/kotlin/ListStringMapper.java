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
package org.jdbi.sqlobject.kotlin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.statement.StatementContext;

public class ListStringMapper implements ColumnMapper<List<String>> {
    @Override
    public List<String> map(ResultSet resultSet, int columnNumber, StatementContext ctx) throws SQLException {
        var data = resultSet.getString(columnNumber);
        if (resultSet.wasNull()) {
            return List.of();
        }
        return List.of(data.split(","));
    }
}

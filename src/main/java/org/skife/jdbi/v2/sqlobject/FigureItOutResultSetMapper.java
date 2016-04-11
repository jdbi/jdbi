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
package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultColumnMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;

class FigureItOutResultSetMapper implements ResultSetMapper<Object> {
    @Override
    public Object map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        Method m = ctx.getSqlObjectMethod();
        Class<?> rt = m.getReturnType();
        GetGeneratedKeys ggk = m.getAnnotation(GetGeneratedKeys.class);
        String keyColumn = ggk.columnName();

        ResultColumnMapper columnMapper = ctx.columnMapperFor(rt);

        if ("".equals(keyColumn)) {
            return columnMapper.mapColumn(r, 1, ctx);
        }

        return columnMapper.mapColumn(r, keyColumn, ctx);
    }
}

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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

class RegisteredMapper<T> implements ResultSetMapper<T>
{

    private final Class<T> type;
    private final MappingRegistry registry;

    RegisteredMapper(Class<T> type, MappingRegistry registry) {
        this.type = type;
        this.registry = registry;
    }

    @Override
    public T map(int index, ResultSet r, StatementContext ctx) throws SQLException
    {
        return (T) registry.mapperFor(type, ctx).map(index, r, ctx);
    }
}

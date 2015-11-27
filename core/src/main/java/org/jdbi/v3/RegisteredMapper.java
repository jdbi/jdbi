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
package org.jdbi.v3;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.common.reflect.TypeToken;
import org.jdbi.v3.tweak.ResultSetMapper;

class RegisteredMapper<T> implements ResultSetMapper<T>
{

    private final TypeToken<T> type;
    private final MappingRegistry registry;

    RegisteredMapper(TypeToken<T> type, MappingRegistry registry) {
        this.type = type;
        this.registry = registry;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T map(int index, ResultSet r, StatementContext ctx) throws SQLException
    {
        return registry.mapperFor(type, ctx).map(index, r, ctx);
    }
}

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
package org.jdbi.v3.core.array;

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

class ArrayColumnMapper implements ColumnMapper<Object> {
    private static final CopyOnWriteArraySet<Integer> UNSUPPORTED_TYPES = new CopyOnWriteArraySet<>();

    private final ColumnMapper<?> elementMapper;
    private final Class<?> componentType;

    ArrayColumnMapper(ColumnMapper<?> elementMapper,
                      Class<?> componentType) {
        this.elementMapper = elementMapper;
        this.componentType = componentType;
    }

    @Override
    public Object map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        java.sql.Array array = r.getArray(columnNumber);

        if (array == null) {
            return null;
        }

        try {
            return buildArray(array, ctx);
        } finally {
            array.free();
        }
    }

    private Object buildArray(java.sql.Array array, StatementContext ctx) throws SQLException {
        if (!UNSUPPORTED_TYPES.contains(array.getBaseType())) {
            try {
                Object ary = array.getArray();
                if (componentType.equals(ary.getClass().getComponentType())) {
                    return ary;
                }
            } catch (SQLFeatureNotSupportedException ignore) {}
        }

        UNSUPPORTED_TYPES.add(array.getBaseType());

        return buildFromResultSet(array, ctx);
    }

    private Object buildFromResultSet(java.sql.Array array, StatementContext ctx) throws SQLException {
        List<Object> list = new ArrayList<>();
        try (ResultSet rs = array.getResultSet()) {
            while (rs.next()) {
                list.add(elementMapper.map(rs, 2, ctx));
            }
        }

        Object ary = Array.newInstance(componentType, list.size());
        if (componentType.isPrimitive()) {
            for (int i = 0; i < list.size(); i++) {
                Array.set(ary, i, list.get(i));
            }
            return ary;
        }

        return list.toArray((Object[]) ary);
    }
}

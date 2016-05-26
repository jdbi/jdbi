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
package org.jdbi.v3.pg;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jdbi.v3.StatementContext;
import org.jdbi.v3.tweak.ColumnMapper;

public class ArrayColumnMapper implements ColumnMapper<Object[]> {
    private static final CopyOnWriteArraySet<Integer> UNSUPPORTED_TYPES = new CopyOnWriteArraySet<>();
    private final Class<?> componentType;
    private final StatementContext ctx;

    public ArrayColumnMapper(Class<?> componentType, StatementContext ctx) {
        this.componentType = componentType;
        this.ctx = ctx;
    }

    @Override
    public Object[] map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        return buildArray(r.getArray(columnNumber));
    }

    private Object[] buildArray(Array array) throws SQLException {
        if (UNSUPPORTED_TYPES.contains(array.getBaseType())) {
            return buildFromResultSet(array);
        }
        try {
            // more efficient, doesn't have to copy an unknown size array
            return (Object[]) array.getArray();
        } catch (SQLFeatureNotSupportedException e) {
            UNSUPPORTED_TYPES.add(array.getBaseType());
            return buildFromResultSet(array);
        }
    }

    private Object[] buildFromResultSet(Array array) throws SQLException {
        final ColumnMapper<?> mapper = ctx.findColumnMapperFor(componentType)
                .orElseThrow(() -> new IllegalArgumentException("Unable to find column mapper for " + componentType));

        int capacity = 16;
        int length = 0;
        Object[] accumulator = (Object[]) java.lang.reflect.Array.newInstance(componentType, capacity);
        try (ResultSet rs = array.getResultSet()) {
            while (rs.next()) {
                accumulator[length++] = mapper.map(rs, 2, ctx);
                if (length == capacity) {
                    Object[] oldArray = accumulator;
                    accumulator = (Object[]) java.lang.reflect.Array.newInstance(componentType, capacity * 2);
                    System.arraycopy(oldArray, 0, accumulator, 0, capacity);
                    capacity *= 2;
                }
            }
        }

        if (length == capacity) {
            return accumulator;
        }
        return Arrays.copyOf(accumulator, length);
    }
}

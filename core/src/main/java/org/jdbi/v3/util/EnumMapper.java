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
package org.jdbi.v3.util;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.StatementContext;
import org.jdbi.v3.tweak.ColumnMapper;

public abstract class EnumMapper<E extends Enum<E>> implements ColumnMapper<E> {
    EnumMapper() {}

    public static <E extends Enum<E>> ColumnMapper<E> byName(Class<E> type) {
        return new ByName<>(type);
    }

    public static <E extends Enum<E>> ColumnMapper<E> byOrdinal(Class<E> type) {
        return new ByOrdinal<>(type);
    }

    private static class ByName<E extends Enum<E>> extends EnumMapper<E> {
        private final Class<E> type;

        private ByName(Class<E> type) {
            this.type = type;
        }

        @Override
        public E map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            String name = r.getString(columnNumber);
            return name == null ? null : Enum.valueOf(type, name);
        }
    }

    private static class ByOrdinal<E extends Enum<E>> extends EnumMapper<E> {
        private final E[] constants;

        private ByOrdinal(Class<E> type) {
            this.constants = type.getEnumConstants();
        }

        @Override
        public E map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            int ordinal = r.getInt(columnNumber);
            return r.wasNull() ? null : constants[ordinal];
        }
    }
}

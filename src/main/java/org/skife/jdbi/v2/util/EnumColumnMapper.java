/*
 * Copyright (C) 2015 Zane Benefits
 *
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
package org.skife.jdbi.v2.util;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultColumnMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class EnumColumnMapper<E extends Enum<E>> implements ResultColumnMapper<E> {
    EnumColumnMapper() {}

    public static <E extends Enum<E>> ResultColumnMapper<E> byName(Class<E> type) {
        return new ByName<E>(type);
    }

    public static <E extends Enum<E>> ResultColumnMapper<E> byOrdinal(Class<E> type) {
        return new ByOrdinal<E>(type);
    }

    private static class ByName<E extends Enum<E>> extends EnumColumnMapper<E> {
        private final Class<E> type;

        private ByName(Class<E> type) {
            this.type = type;
        }

        @Override
        public E mapColumn(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            String name = r.getString(columnNumber);
            return name == null ? null : Enum.valueOf(type, name);
        }

        @Override
        public E mapColumn(ResultSet r, String columnLabel, StatementContext ctx) throws SQLException {
            String name = r.getString(columnLabel);
            return name == null ? null : Enum.valueOf(type, name);
        }
    }

    private static class ByOrdinal<E extends Enum<E>> extends EnumColumnMapper<E> {
        private final E[] constants;

        private ByOrdinal(Class<E> type) {
            this.constants = type.getEnumConstants();
        }

        @Override
        public E mapColumn(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            int ordinal = r.getInt(columnNumber);
            return r.wasNull() ? null : constants[ordinal];
        }

        @Override
        public E mapColumn(ResultSet r, String columnLabel, StatementContext ctx) throws SQLException {
            int ordinal = r.getInt(columnLabel);
            return r.wasNull() ? null : constants[ordinal];
        }
    }
}

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
import org.jdbi.v3.tweak.ResultColumnMapper;

/**
 * Map JDBC column value to a Java <code>char</code> or <code>Character</code>.
 */
public enum CharMapper implements ResultColumnMapper<Character> {
    /**
     * Map to <code>char</code>.  Nulls and empty strings are silently replaced with a null char <code>'\0'</code>.
     */
    PRIMITIVE(false),
    /**
     * Map to <code>Character</code>.  Nulls and empty strings are replaced with a <code>null</code> reference.
     */
    WRAPPER(true);

    private final boolean nullable;

    CharMapper(boolean nullable) {
        this.nullable = nullable;
    }

    @Override
    public Character mapColumn(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        return charFromString(r.getString(columnNumber));
    }

    public Character charFromString(String s) {
        if (s != null && !s.isEmpty()) {
            return s.charAt(0);
        }
        if (nullable) {
            return null;
        }
        return '\000';
    }
}

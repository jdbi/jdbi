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
package org.jdbi.v3.core.argument;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jdbi.v3.core.StatementContext;

/**
 * An Argument which uses {@code setObject} to support
 * vendor specific types.
 */
public class ObjectArgument implements Argument<Object> {
    private final Integer sqlType;

    public ObjectArgument(int sqlType) {
        this.sqlType = sqlType;
    }

    @Override
    public void apply(PreparedStatement statement, int position, Object value, StatementContext ctx) throws SQLException {
        statement.setObject(position, value, sqlType);
    }

    @Override
    public String toString() {
        return "ObjectArgument{sqlType=" + sqlType + "}";
    }
}

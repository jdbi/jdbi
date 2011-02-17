/*
 * Copyright 2004 - 2011 Brian McCallister
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

package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class SqlTypeArgument implements Argument
{
    private final Object value;
    private final int sqlType;

    public SqlTypeArgument(Object value, int sqlType)
    {
        this.value = value;
        this.sqlType = sqlType;
    }

    public void apply(final int position, PreparedStatement statement, StatementContext ctx) throws SQLException
    {
        statement.setObject(position, value, sqlType);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

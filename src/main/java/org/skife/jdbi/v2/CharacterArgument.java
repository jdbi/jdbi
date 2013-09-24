/*
 * Copyright (C) 2004 - 2013 Brian McCallister
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
import java.sql.Types;

class CharacterArgument implements Argument
{
    private final Character value;

    CharacterArgument(Character value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException
    {
        if (value != null) {
            statement.setString(position, String.valueOf(value));
        }
        else {
            statement.setNull(position, Types.CHAR);
        }
    }

    @Override
    public String toString()
    {
        return String.valueOf(value);
    }
}
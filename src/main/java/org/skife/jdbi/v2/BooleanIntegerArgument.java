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

import org.skife.jdbi.v2.tweak.Argument;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Takes a boolean and converts it into integer 0/1 column values. This is useful if your database does
 * not support boolean column types.
 */
class BooleanIntegerArgument implements Argument
{
    private final boolean value;

    BooleanIntegerArgument(final boolean value)
    {
        this.value = value;
    }

    @Override
    public void apply(final int position, final PreparedStatement statement, final StatementContext ctx) throws SQLException
    {
        statement.setInt(position, value ? 1 : 0);
    }

    @Override
    public String toString()
    {
        return String.valueOf(value);
    }
}

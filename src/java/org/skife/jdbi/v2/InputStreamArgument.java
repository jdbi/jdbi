/*
 * Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class InputStreamArgument implements Argument
{
    private final InputStream value;
    private final int length;
    private final boolean ascii;

    InputStreamArgument(InputStream value, int length, boolean ascii)
    {
        this.value = value;
        this.length = length;
        this.ascii = ascii;
    }

    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException
    {
        if (ascii)
        {
            statement.setAsciiStream(position, value, length);
        }
        else
        {
            statement.setBinaryStream(position, value, length);
        }
    }
}

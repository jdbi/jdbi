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

import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;

/**
 * Bind a {@link Reader} as a character stream parameter.
 */
public class CharacterStreamArgument implements Argument
{
    private final Reader value;
    private final int length;

    /**
     * @param reader the character stream to bind
     * @param length the length of the stream
     */
    public CharacterStreamArgument(Reader reader, int length)
    {
        this.value = reader;
        this.length = length;
    }

    @Override
    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException
    {
        statement.setCharacterStream(position, value, length);
    }
}

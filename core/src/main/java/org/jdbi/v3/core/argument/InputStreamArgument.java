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

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.jdbi.v3.core.statement.StatementContext;

/**
 * Bind an input stream as either an ASCII (discouraged) or binary stream.
 */
public class InputStreamArgument implements Argument {
    private final InputStream value;
    private final int length;
    private final boolean ascii;

    /**
     * @param stream the stream to bind
     * @param length the length of the stream
     * @param ascii true if the stream is ASCII
     */
    public InputStreamArgument(InputStream stream, int length, boolean ascii) {
        this.value = stream;
        this.length = length;
        this.ascii = ascii;
    }

    @Override
    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
        if (ascii) {
            if (value == null) {
                statement.setNull(position, Types.LONGVARCHAR);
            } else {
                statement.setAsciiStream(position, value, length);
            }
        } else {
            if (value == null) {
                statement.setNull(position, Types.LONGVARBINARY);
            } else {
                statement.setBinaryStream(position, value, length);
            }
        }
    }
}

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
package org.jdbi.v3.core.array;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.junit5.DatabaseExtension;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ByteArrayTest {

    @RegisterExtension
    public DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @Mock
    private PreparedStatement stmt;

    StatementContext ctx = StatementContextAccess.createContext();

    @Test
    public void byteArrayIsTypedAsVarbinary() throws SQLException {
        Argument nullByteArrayArg = h2Extension.getJdbi().withHandle(h -> h.getConfig(Arguments.class).findFor(byte[].class, new byte[]{1})).get();

        nullByteArrayArg.apply(0, stmt, ctx);

        verify(stmt, never()).setArray(anyInt(), any(Array.class));
        verify(stmt).setBytes(anyInt(), any(byte[].class));
    }

    @Test
    public void nullByteArrayIsTypedAsVarbinary() throws SQLException {
        Argument nullByteArrayArg = h2Extension.getJdbi().withHandle(h -> h.getConfig(Arguments.class).findFor(byte[].class, null)).get();

        nullByteArrayArg.apply(0, stmt, ctx);

        verify(stmt, never()).setNull(anyInt(), eq(Types.ARRAY));
        verify(stmt).setNull(anyInt(), eq(Types.VARBINARY));
    }
}

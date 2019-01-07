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
import org.jdbi.v3.core.rule.DatabaseRule;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ByteArrayTest {
    @Rule
    public DatabaseRule db = new H2DatabaseRule();
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();
    @Mock
    private PreparedStatement stmt;

    @Test
    public void byteArrayIsTypedAsVarbinary() throws SQLException {
        Argument nullByteArrayArg = db.getJdbi().withHandle(h -> h.getConfig(Arguments.class).findFor(byte[].class, new byte[] {1})).get();

        nullByteArrayArg.apply(0, stmt, null);

        verify(stmt, never()).setArray(anyInt(), any(Array.class));
        verify(stmt).setBytes(anyInt(), any(byte[].class));
    }

    @Test
    public void nullByteArrayIsTypedAsVarbinary() throws SQLException {
        Argument nullByteArrayArg = db.getJdbi().withHandle(h -> h.getConfig(Arguments.class).findFor(byte[].class, null)).get();

        nullByteArrayArg.apply(0, stmt, null);

        verify(stmt, never()).setNull(anyInt(), eq(Types.ARRAY));
        verify(stmt).setNull(anyInt(), eq(Types.VARBINARY));
    }
}

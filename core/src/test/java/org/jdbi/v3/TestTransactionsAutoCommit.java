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
package org.jdbi.v3;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.Test;
import org.mockito.InOrder;

public class TestTransactionsAutoCommit
{

    public static final String SAMPLE_SQL = "insert into something (id, name) values (?, ?)";

    @Test
    public void restoreAutoCommitInitialStateOnUnexpectedError() throws Exception
    {

        final Connection connection = mock(Connection.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        InOrder inOrder = inOrder(connection, statement);

        Handle h = DBI.create(() -> connection).open();

        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.execute()).thenReturn(true);
        when(statement.getUpdateCount()).thenReturn(1);
        // throw e.g some underlying database error
        doThrow(new SQLException("infrastructure error")).when(connection).commit();

        h.begin();
        try {
            h.insert(SAMPLE_SQL, 1L, "Tom");

            // throws exception on commit
            h.commit();
        } catch (final Exception exception) {
            // ignore
        }

        // expected behaviour chain:
        // 1. store initial auto-commit state
        inOrder.verify(connection).getAutoCommit();

        // 2. turn off auto-commit
        inOrder.verify(connection).setAutoCommit(false);

        // 3. execute statement (without commit)
        inOrder.verify(connection).prepareStatement("insert into something (id, name) values (?, ?)");
        inOrder.verify(statement).execute();
        inOrder.verify(statement).getUpdateCount();

        // 4. commit transaction
        inOrder.verify(connection).commit();

        // 5. set auto-commit back to initial state
        inOrder.verify(connection).setAutoCommit(true);
    }

}

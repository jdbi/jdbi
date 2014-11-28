/*
 * Copyright (C) 2004 - 2014 Brian McCallister
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

import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class TestTransactionsAutoCommit extends DBITestCase
{
    @Test
    public void restoreAutoCommitInitialStateOnUnexpectedError() throws Exception
    {

        final Connection connection = createNiceMock(Connection.class);
        final PreparedStatement statement = createNiceMock(PreparedStatement.class);

        Handle h = openHandle(connection);

        // expected behaviour chain:
        // 1. store initial auto-commit state
        expect(connection.getAutoCommit()).andReturn(true);

        // 2. turn off auto-commit
        connection.setAutoCommit(false);
        expectLastCall().once();

        // 3. execute statement (without commit)
        expect(connection.prepareStatement("insert into something (id, name) values (?, ?)")).andReturn(statement);
        expect(statement.execute()).andReturn(true);
        expect(statement.getUpdateCount()).andReturn(1);

        // 4. commit transaction (throw e.g some underlying database error)
        connection.commit();
        expectLastCall().andThrow(new SQLException("infrastructure error"));

        // 5. set auto-commit back to initial state
        connection.setAutoCommit(true);
        expectLastCall().once();

        replay(connection, statement);

        h.begin();
        try {
            h.insert("insert into something (id, name) values (?, ?)", 1L, "Tom");

            // throws exception on commit
            h.commit();
        } catch (final Exception exception) {
            // ignore
        }

        verify(connection);
    }

}

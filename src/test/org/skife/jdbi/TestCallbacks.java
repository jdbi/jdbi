/* Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi;

import junit.framework.TestCase;
import org.skife.jdbi.derby.Tools;

import java.sql.Connection;
import java.util.Map;

public class TestCallbacks extends TestCase
{
    private Handle handle;
    private Connection conn;

    public void setUp() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
        handle = DBI.open(Tools.CONN_STRING);
        conn = handle.getConnection();
    }

    public void tearDown() throws Exception
    {
        handle.close();
        Tools.stop();
    }

    public void testSelectCallbackHasCorrectValues() throws Exception
    {
        handle.execute("insert into something (id, name) values (1, 'george')");
        final int[] counter = {0};
        handle.query("select id, name from something", new RowCallback()
        {
            public void eachRow(Handle handle, Map row)
            {
                counter[0]++;
                assertEquals(new Integer(1), row.get("id"));
                assertEquals("george", row.get("name"));
            }
        });
        assertEquals(1, counter[0]);
    }

    public void testQueryCallbackIsNotInTransaction() throws Exception
    {
        handle.execute("insert into something (id, name) values (2, 'bobby')");
        handle.query("select id, name from something order by id", new RowCallback()
        {
            public void eachRow(Handle handle, Map row) throws Exception
            {
                assertTrue("autocommit should be false for this", conn.getAutoCommit());
            }
        });
    }

    public void testExceptionOnQueryCallbackRollsQueryBack() throws Exception
    {
        handle.execute("insert into something (id, name) values (2, 'bobby')");
        try
        {
            handle.query("select id, name from something order by id", new RowCallback()
            {
                public void eachRow(Handle handle, Map row) throws Exception
                {
                    throw new NullPointerException("aaa");
                }
            });
            fail("should have thrown exception");
        }
        catch (DBIException e)
        {
            final Throwable cause = e.getCause();
            assertTrue(cause instanceof NullPointerException);
            assertEquals("aaa", cause.getMessage());
            assertFalse(handle.isInTransaction());
        }
    }

    public void testUseTransactionAlreadyInProgress() throws Exception
    {
        conn.setAutoCommit(false);
        handle.execute("insert into something (id, name) values (2, 'bobby')");
        handle.query("select id, name from something order by id", new RowCallback()
        {
            public void eachRow(Handle handle, Map row) throws Exception
            {
                // do nothing
            }
        });
        assertFalse(conn.getAutoCommit());
        conn.commit();
    }

    public void testInTransaction() throws Exception
    {
        final boolean called[] = { false };
        assertTrue(handle.isOpen());
        assertFalse(handle.isInTransaction());
        handle.inTransaction(new TransactionCallback()
        {
            public void inTransaction(Handle handle) throws Exception
            {
                called[0] = true;
                assertTrue(handle.isInTransaction());
                assertTrue(handle.isOpen());
            }
        });
        assertTrue(called[0]);
        assertTrue(handle.isOpen());
        assertFalse(handle.isInTransaction());
    }
}

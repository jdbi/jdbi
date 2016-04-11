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

import org.junit.Test;
import org.skife.jdbi.v2.exceptions.DBIException;
import org.skife.jdbi.v2.exceptions.TransactionException;
import org.skife.jdbi.v2.exceptions.TransactionFailedException;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestTransactions extends DBITestCase
{
    @Test
    public void testCallback() throws Exception
    {
        Handle h = this.openHandle();

        String woot = h.inTransaction(new TransactionCallback<String>()
        {
            @Override
            public String inTransaction(Handle handle, TransactionStatus status) throws Exception
            {
                return "Woot!";
            }
        });

        assertEquals("Woot!", woot);
    }

    @Test
    public void testRollbackOutsideTx() throws Exception
    {
        Handle h = openHandle();

        h.insert("insert into something (id, name) values (?, ?)", 7, "Tom");
        h.rollback();
    }

    @Test
    public void testDoubleOpen() throws Exception
    {
        Handle h = openHandle();
        assertTrue(h.getConnection().getAutoCommit());

        h.begin();
        h.begin();
        assertFalse(h.getConnection().getAutoCommit());
        h.commit();
        assertTrue(h.getConnection().getAutoCommit());
    }

    @Test
    public void testExceptionAbortsTransaction() throws Exception
    {
        Handle h = this.openHandle();

        try
        {
            h.inTransaction(new TransactionCallback<Object>()
            {
                @Override
                public Object inTransaction(Handle handle, TransactionStatus status) throws Exception
                {
                    handle.insert("insert into something (id, name) values (:id, :name)", 0, "Keith");

                    throw new IOException();
                }
            });
            fail("Should have thrown exception");
        }
        catch (TransactionFailedException e)
        {
            assertTrue(true);
        }

        List<Something> r = h.createQuery("select * from something").map(Something.class).list();
        assertEquals(0, r.size());
    }

    @Test
    public void testRollbackOnlyAbortsTransaction() throws Exception
    {
        Handle h = this.openHandle();

        try
        {
            h.inTransaction(new TransactionCallback<Object>()
            {
                @Override
                public Object inTransaction(Handle handle, TransactionStatus status) throws Exception
                {
                    handle.insert("insert into something (id, name) values (:id, :name)", 0, "Keith");
                    status.setRollbackOnly();
                    return "Hi";
                }
            });
            fail("Should have thrown exception");
        }
        catch (TransactionFailedException e)
        {
            assertTrue(true);
        }

        List<Something> r = h.createQuery("select * from something").map(Something.class).list();
        assertEquals(0, r.size());
    }

    @Test
    public void testCheckpoint() throws Exception
    {
        Handle h = openHandle();
        h.begin();

        h.insert("insert into something (id, name) values (:id, :name)", 1, "Tom");
        h.checkpoint("first");
        h.insert("insert into something (id, name) values (:id, :name)", 1, "Martin");
        assertEquals(Integer.valueOf(2), h.createQuery("select count(*) from something").mapTo(Integer.class).first());
        h.rollback("first");
        assertEquals(Integer.valueOf(1), h.createQuery("select count(*) from something").mapTo(Integer.class).first());
        h.commit();
        assertEquals(Integer.valueOf(1), h.createQuery("select count(*) from something").mapTo(Integer.class).first());
    }

    @Test
    public void testReleaseCheckpoint() throws Exception
    {
        Handle h = openHandle();
        h.begin();
        h.checkpoint("first");
        h.insert("insert into something (id, name) values (:id, :name)", 1, "Martin");

        h.release("first");

        try {
            h.rollback("first");
            fail("Should have thrown an exception of some kind");
        }
        catch (TransactionException e) {
            h.rollback();
            assertTrue(true);
        }
    }

    @Test
    public void testThrowingRuntimeExceptionPercolatesOriginal() throws Exception
    {
        Handle h = openHandle();
        try {
            h.inTransaction(new TransactionCallback<Object>() {
                @Override
                public Object inTransaction(Handle handle, TransactionStatus status) throws Exception
                {
                    throw new IllegalArgumentException();
                }
            });
        }
        catch (DBIException e) {
            fail("Should have thrown a straight RuntimeException");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals("Go here", 2, 1 + 1);
        }
        catch (Exception e) {
            fail("Should have been caught at IllegalArgumentException");
        }
    }
}

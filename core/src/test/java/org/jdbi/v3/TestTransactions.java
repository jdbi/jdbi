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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import org.jdbi.v3.exceptions.DBIException;
import org.jdbi.v3.exceptions.TransactionException;
import org.jdbi.v3.exceptions.TransactionFailedException;
import org.junit.Rule;
import org.junit.Test;

public class TestTransactions
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testCallback() throws Exception
    {
        Handle h = db.openHandle();

        String woot = h.inTransaction((handle, status) -> "Woot!");

        assertEquals("Woot!", woot);
    }

    @Test
    public void testRollbackOutsideTx() throws Exception
    {
        Handle h = db.openHandle();

        h.insert("insert into something (id, name) values (?, ?)", 7, "Tom");
        h.rollback();
    }

    @Test
    public void testDoubleOpen() throws Exception
    {
        Handle h = db.openHandle();
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
        Handle h = db.openHandle();

        try
        {
            h.inTransaction((handle, status) -> {
                handle.insert("insert into something (id, name) values (:id, :name)", 0, "Keith");

                throw new IOException();
            });
            fail("Should have thrown exception");
        }
        catch (TransactionFailedException e)
        {
            assertTrue(true);
        }

        List<Something> r = h.createQuery("select * from something").mapToBean(Something.class).list();
        assertEquals(0, r.size());
    }

    @Test
    public void testRollbackOnlyAbortsTransaction() throws Exception
    {
        Handle h = db.openHandle();

        try
        {
            h.inTransaction((handle, status) -> {
                handle.insert("insert into something (id, name) values (:id, :name)", 0, "Keith");
                status.setRollbackOnly();
                return "Hi";
            });
            fail("Should have thrown exception");
        }
        catch (TransactionFailedException e)
        {
            assertTrue(true);
        }

        List<Something> r = h.createQuery("select * from something").mapToBean(Something.class).list();
        assertEquals(0, r.size());
    }

    @Test
    public void testCheckpoint() throws Exception
    {
        Handle h = db.openHandle();
        h.begin();

        h.insert("insert into something (id, name) values (:id, :name)", 1, "Tom");
        h.checkpoint("first");
        h.insert("insert into something (id, name) values (:id, :name)", 2, "Martin");
        assertEquals(Integer.valueOf(2), h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly());
        h.rollback("first");
        assertEquals(Integer.valueOf(1), h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly());
        h.commit();
        assertEquals(Integer.valueOf(1), h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly());
    }

    @Test
    public void testReleaseCheckpoint() throws Exception
    {
        Handle h = db.openHandle();
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
        Handle h = db.openHandle();
        try {
            h.inTransaction((handle, status) -> {
                throw new IllegalArgumentException();
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

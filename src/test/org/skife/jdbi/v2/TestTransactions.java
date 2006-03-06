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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.TransactionFailedException;

import java.io.IOException;
import java.util.List;

public class TestTransactions extends DBITestCase
{
    public void testCallback() throws Exception
    {
        Handle h = this.openHandle();

        String woot = h.inTransaction(new TransactionCallback<String>()
        {
            public String inTransaction(Handle handle, TransactionStatus status) throws Exception
            {
                return "Woot!";
            }
        });

        assertEquals("Woot!", woot);
    }

    public void testExceptionAbortsTransaction() throws Exception
    {
        Handle h = this.openHandle();

        try
        {
            h.inTransaction(new TransactionCallback<Object>()
            {
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

    public void testRollbackOnlyAbortsTransaction() throws Exception
    {
        Handle h = this.openHandle();

        try
        {
            h.inTransaction(new TransactionCallback<Object>()
            {
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
}

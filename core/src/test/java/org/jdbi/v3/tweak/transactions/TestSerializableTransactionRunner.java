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
package org.jdbi.v3.tweak.transactions;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.MemoryDatabase;
import org.jdbi.v3.TransactionCallback;
import org.jdbi.v3.TransactionIsolationLevel;
import org.jdbi.v3.TransactionStatus;
import org.jdbi.v3.exceptions.TransactionFailedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestSerializableTransactionRunner
{
    @Rule
    public MemoryDatabase db = new MemoryDatabase();

    private DBI dbi;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI(db.getDataSource());
        dbi.setTransactionHandler(new SerializableTransactionRunner());
    }

    @Test
    public void testEventuallyFails() throws Exception
    {
        final AtomicInteger tries = new AtomicInteger(5);
        Handle handle = dbi.open();

        try {
            handle.inTransaction(TransactionIsolationLevel.SERIALIZABLE, new TransactionCallback<Void>() {
                @Override
                public Void inTransaction(Handle conn, TransactionStatus status) throws Exception
                {
                    tries.decrementAndGet();
                    throw new SQLException("serialization", "40001");
                }
            });
        } catch (TransactionFailedException e)
        {
            Assert.assertEquals("40001", ((SQLException) e.getCause()).getSQLState());
        }
        Assert.assertEquals(0, tries.get());
    }

    @Test
    public void testEventuallySucceeds() throws Exception
    {
        final AtomicInteger tries = new AtomicInteger(3);
        Handle handle = dbi.open();

        handle.inTransaction(TransactionIsolationLevel.SERIALIZABLE, new TransactionCallback<Void>() {
            @Override
            public Void inTransaction(Handle conn, TransactionStatus status) throws Exception
            {
                if (tries.decrementAndGet() == 0)
                {
                    return null;
                }
                throw new SQLException("serialization", "40001");
            }
        });

        Assert.assertEquals(0, tries.get());
    }
}

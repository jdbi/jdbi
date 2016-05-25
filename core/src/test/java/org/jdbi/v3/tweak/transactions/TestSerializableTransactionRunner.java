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

import org.jdbi.v3.Jdbi;
import org.jdbi.v3.Handle;
import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.TransactionIsolationLevel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestSerializableTransactionRunner
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private Jdbi dbi;

    @Before
    public void setUp() throws Exception
    {
        dbi = Jdbi.create(db.getConnectionFactory());
        dbi.setTransactionHandler(new SerializableTransactionRunner());
    }

    @Test
    public void testEventuallyFails() throws Exception
    {
        final AtomicInteger tries = new AtomicInteger(5);
        Handle handle = dbi.open();

        try {
            handle.inTransaction(TransactionIsolationLevel.SERIALIZABLE, (conn, status) -> {
                tries.decrementAndGet();
                throw new SQLException("serialization", "40001");
            });
        } catch (SQLException e)
        {
            Assert.assertEquals("40001", e.getSQLState());
        }
        Assert.assertEquals(0, tries.get());
    }

    @Test
    public void testEventuallySucceeds() throws Exception
    {
        final AtomicInteger tries = new AtomicInteger(3);
        Handle handle = dbi.open();

        handle.inTransaction(TransactionIsolationLevel.SERIALIZABLE, (conn, status) -> {
            if (tries.decrementAndGet() == 0)
            {
                return null;
            }
            throw new SQLException("serialization", "40001");
        });

        Assert.assertEquals(0, tries.get());
    }
}

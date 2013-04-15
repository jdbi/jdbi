/*
 * Copyright 2004 - 2012 Brian McCallister
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.skife.jdbi.v2.sqlobject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.exceptions.TransactionFailedException;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TestTransactionAnnotation
{
    private DBI dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        UUID uuid = UUID.randomUUID();
        dbi = new DBI("jdbc:h2:mem:" + uuid);
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");

    }

    @After
    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }

    @Test
    public void testTx() throws Exception
    {
        Dao dao = handle.attach(Dao.class);
        Something s = dao.insertAndFetch(1, "Ian");
        assertThat(s, equalTo(new Something(1, "Ian")));
    }

    @Test
    public void testTxFail() throws Exception
    {
        Dao dao = handle.attach(Dao.class);

        try {
            dao.failed(1, "Ian");
            fail("should have raised exception");
        }
        catch (TransactionFailedException e) {
            assertThat(e.getCause().getMessage(), equalTo("woof"));
        }
        assertThat(dao.findById(1), nullValue());
    }

    @Test
    public void testNestedTransactions() throws Exception
    {
        Dao dao = handle.attach(Dao.class);

        Something s = dao.insertAndFetchWithNestedTransaction(1, "Ian");
        assertThat(s, equalTo(new Something(1, "Ian")));

    }

    @Test
    public void testTxActuallyCommits() throws Exception
    {
        Handle h2 = dbi.open();
        Dao one = handle.attach(Dao.class);
        Dao two = h2.attach(Dao.class);

        // insert in @Transaction method
        Something inserted = one.insertAndFetch(1, "Brian");

        // fetch from another connection
        Something fetched = two.findById(1);
        assertThat(fetched, equalTo(inserted));
    }

    @RegisterMapper(SomethingMapper.class)
    public static abstract class Dao
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public abstract void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        public abstract Something findById(@Bind("id") int id);

        @Transaction(TransactionIsolationLevel.READ_COMMITTED)
        public Something insertAndFetch(int id, String name)
        {
            insert(id, name);
            return findById(id);
        }

        @Transaction
        public Something insertAndFetchWithNestedTransaction(int id, String name)
        {
            return insertAndFetch(id, name);
        }

        @Transaction
        public Something failed(int id, String name) throws IOException
        {
            insert(id, name);
            throw new IOException("woof");
        }
    }
}

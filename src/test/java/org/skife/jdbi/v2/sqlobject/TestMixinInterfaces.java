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
package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;
import org.skife.jdbi.v2.sqlobject.mixins.GetHandle;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.tweak.HandleCallback;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

public class TestMixinInterfaces
{
    private DBI dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(String.format("jdbc:h2:mem:%s;MVCC=TRUE", UUID.randomUUID()));
        dbi = new DBI(ds);
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
    public void testGetHandle() throws Exception
    {
        WithGetHandle g = SqlObjectBuilder.attach(handle, WithGetHandle.class);
        Handle h = g.getHandle();

        assertSame(handle, h);
    }

    @Test
    public void testWithHandle() throws Exception
    {
        WithGetHandle g = SqlObjectBuilder.attach(handle, WithGetHandle.class);
        String name = g.withHandle(new HandleCallback<String>() {
            @Override
            public String withHandle(Handle handle) throws Exception {
                handle.execute("insert into something (id, name) values (8, 'Mike')");

                return handle.createQuery("select name from something where id = 8").mapTo(String.class).first();
            }
        });

        assertEquals("Mike", name);
    }

    @Test
    public void testBeginAndCommitTransaction() throws Exception
    {
        TransactionStuff txl = SqlObjectBuilder.attach(handle, TransactionStuff.class);

        txl.insert(8, "Mike");

        txl.begin();
        txl.updateName(8, "Miker");
        assertEquals("Miker", txl.byId(8).getName());
        txl.rollback();

        assertEquals("Mike", txl.byId(8).getName());

    }

    @Test
    public void testInTransaction() throws Exception
    {
        TransactionStuff txl = SqlObjectBuilder.attach(handle, TransactionStuff.class);
        txl.insert(7, "Keith");

        Something s = txl.inTransaction(new Transaction<Something, TransactionStuff>() {
            @Override
            public Something inTransaction(TransactionStuff conn, TransactionStatus status) throws Exception
            {
                return conn.byId(7);
            }
        });

        assertEquals("Keith", s.getName());
    }

    @Test
    public void testInTransactionWithLevel() throws Exception
    {
        TransactionStuff txl = SqlObjectBuilder.attach(handle, TransactionStuff.class);
        txl.insert(7, "Keith");

        Something s = txl.inTransaction(TransactionIsolationLevel.SERIALIZABLE, new Transaction<Something, TransactionStuff>() {
            @Override
            public Something inTransaction(TransactionStuff conn, TransactionStatus status) throws Exception
            {
                Assert.assertEquals(TransactionIsolationLevel.SERIALIZABLE, conn.getHandle().getTransactionIsolationLevel());
                return conn.byId(7);
            }
        });

        assertEquals("Keith", s.getName());
    }

    @Test
    public void testTransactionIsolationActuallyHappens() throws Exception
    {
        TransactionStuff txl = SqlObjectBuilder.attach(handle, TransactionStuff.class);
        TransactionStuff tx2 = SqlObjectBuilder.open(dbi, TransactionStuff.class);


        txl.insert(8, "Mike");

        txl.begin();

        txl.updateName(8, "Miker");
        assertEquals("Miker", txl.byId(8).getName());
        assertEquals("Mike", tx2.byId(8).getName());

        txl.commit();

        assertEquals("Miker", tx2.byId(8).getName());

        tx2.close();
    }

    @Test
    public void testJustJdbiTransactions() throws Exception
    {
        Handle h1 = dbi.open();
        Handle h2 = dbi.open();

        h1.execute("insert into something (id, name) values (8, 'Mike')");

        h1.begin();
        h1.execute("update something set name = 'Miker' where id = 8");

        assertEquals("Mike", h2.createQuery("select name from something where id = 8").mapTo(String.class).first());
        h1.commit();
        h1.close();
        h2.close();
    }

    @Test
    public void testTransmogrifiable() throws Exception
    {
        Hobbsian h = handle.attach(Hobbsian.class);
        h.insert(2, "Cora");
        Something s = h.become(TransactionStuff.class).byId(2);
        assertThat(s, equalTo(new Something(2, "Cora")));
    }

    public static interface WithGetHandle extends CloseMe, GetHandle
    {

    }

    public static interface Hobbsian extends org.skife.jdbi.v2.sqlobject.mixins.Transmogrifier
    {

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@Bind("id") int id, @Bind("name") String name);
    }

    public static interface TransactionStuff extends CloseMe, Transactional<TransactionStuff>, GetHandle
    {

        @SqlQuery("select id, name from something where id = :id")
        @Mapper(SomethingMapper.class)
        public Something byId(@Bind("id") long id);

        @SqlUpdate("update something set name = :name where id = :id")
        public void updateName(@Bind("id") long id, @Bind("name") String name);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@Bind("id") long id, @Bind("name") String name);
    }
}


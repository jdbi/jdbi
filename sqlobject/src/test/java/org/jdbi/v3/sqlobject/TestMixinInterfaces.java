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
package org.jdbi.v3.sqlobject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Something;
import org.jdbi.v3.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.customizers.Mapper;
import org.jdbi.v3.sqlobject.mixins.CloseMe;
import org.jdbi.v3.sqlobject.mixins.GetHandle;
import org.jdbi.v3.sqlobject.mixins.Transactional;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
        String name = g.withHandle(handle1 -> {
            handle1.execute("insert into something (id, name) values (8, 'Mike')");

            return handle1.createQuery("select name from something where id = 8").mapTo(String.class).findOnly();
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

        Something s = txl.inTransaction((conn, status) -> conn.byId(7));

        assertEquals("Keith", s.getName());
    }

    @Test
    public void testInTransactionWithLevel() throws Exception
    {
        TransactionStuff txl = SqlObjectBuilder.attach(handle, TransactionStuff.class);
        txl.insert(7, "Keith");

        Something s = txl.inTransaction(TransactionIsolationLevel.SERIALIZABLE, (conn, status) -> {
            Assert.assertEquals(TransactionIsolationLevel.SERIALIZABLE, conn.getHandle().getTransactionIsolationLevel());
            return conn.byId(7);
        });

        assertEquals("Keith", s.getName());
    }

    @Test
    public void testTransactionIsolationActuallyHappens() throws Exception
    {
        TransactionStuff txl = SqlObjectBuilder.attach(handle, TransactionStuff.class);
        try (TransactionStuff tx2 = SqlObjectBuilder.open(dbi, TransactionStuff.class)) {
            txl.insert(8, "Mike");

            txl.begin();

            txl.updateName(8, "Miker");
            assertEquals("Miker", txl.byId(8).getName());
            assertEquals("Mike", tx2.byId(8).getName());

            txl.commit();

            assertEquals("Miker", tx2.byId(8).getName());
        }
    }

    @Test
    public void testJustJdbiTransactions() throws Exception
    {
        try (Handle h1 = dbi.open();
             Handle h2 = dbi.open()) {
            h1.execute("insert into something (id, name) values (8, 'Mike')");

            h1.begin();
            h1.execute("update something set name = 'Miker' where id = 8");

            assertEquals("Mike", h2.createQuery("select name from something where id = 8").mapTo(String.class).findOnly());
            h1.commit();
        }
    }

    public interface WithGetHandle extends CloseMe, GetHandle
    {

    }

    public interface TransactionStuff extends CloseMe, Transactional<TransactionStuff>, GetHandle
    {

        @SqlQuery("select id, name from something where id = :id")
        @Mapper(SomethingMapper.class)
        Something byId(@Bind("id") long id);

        @SqlUpdate("update something set name = :name where id = :id")
        void updateName(@Bind("id") long id, @Bind("name") String name);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") long id, @Bind("name") String name);
    }
}


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
package org.skife.jdbi.v2.sqlobject;

import org.easymock.EasyMock;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.exceptions.TransactionException;
import org.skife.jdbi.v2.exceptions.UnableToCloseResourceException;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.mixins.GetHandle;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.util.StringMapper;

import java.util.Iterator;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;


public class TestOnDemandSqlObject
{
    private DBI    dbi;
    private Handle handle;
    private JdbcDataSource ds;

    @Before
    public void setUp() throws Exception
    {
        ds = new JdbcDataSource();
        // in MVCC mode h2 doesn't shut down immediately on all connections closed, so need random db name
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
    public void testAPIWorks() throws Exception
    {
        Spiffy s = SqlObjectBuilder.onDemand(dbi, Spiffy.class);

        s.insert(7, "Bill");

        String bill = handle.createQuery("select name from something where id = 7").map(StringMapper.FIRST).first();

        assertEquals("Bill", bill);
    }

    @Test
    public void testTransactionBindsTheHandle() throws Exception
    {
        TransactionStuff txl = SqlObjectBuilder.onDemand(dbi, TransactionStuff.class);
        TransactionStuff tx2 = SqlObjectBuilder.onDemand(dbi, TransactionStuff.class);

        txl.insert(8, "Mike");

        txl.begin();

        assertSame(txl.getHandle(), txl.getHandle());

        txl.updateName(8, "Miker");
        assertEquals("Miker", txl.byId(8).getName());
        assertEquals("Mike", tx2.byId(8).getName());

        txl.commit();

        assertNotSame(txl.getHandle(), txl.getHandle());

        assertEquals("Miker", tx2.byId(8).getName());
    }

    @Test
    public void testIteratorBindsTheHandle() throws Exception
    {
        Spiffy s = SqlObjectBuilder.onDemand(dbi, Spiffy.class);

        s.insert(1, "Tom");
        s.insert(2, "Sam");

        assertNotSame(s.getHandle(), s.getHandle());

        Iterator<Something> all = s.findAll();
        assertSame(s.getHandle(), s.getHandle());

        all.next();
        assertSame(s.getHandle(), s.getHandle());
        all.next();
        assertFalse(all.hasNext());

        assertNotSame(s.getHandle(), s.getHandle());

    }

    @Test(expected=TransactionException.class)
    public void testExceptionOnClose() throws Exception {
        DBI dbi = new DBI(ds) {
            @Override
            public Handle open() {
                Handle h = EasyMock.createMock(Handle.class);
                h.createStatement(EasyMock.anyString());
                EasyMock.expectLastCall()
                    .andThrow(new TransactionException("connection reset"));
                h.close();
                EasyMock.expectLastCall()
                    .andThrow(new UnableToCloseResourceException("already closed", null));
                EasyMock.replay(h);
                return h;
            }
        };

        Spiffy s = SqlObjectBuilder.onDemand(dbi, Spiffy.class);
        s.insert(1, "Tom");
    }

    @Test
    public void testSqlFromExternalFileWorks() throws Exception
    {
        Spiffy spiffy = SqlObjectBuilder.onDemand(dbi, Spiffy.class);
        ExternalSql external = SqlObjectBuilder.onDemand(dbi, ExternalSql.class);

        spiffy.insert(1, "Tom");
        spiffy.insert(2, "Sam");

        Iterator<Something> all = external.findAll();

        all.next();
        all.next();
        assertFalse(all.hasNext());
    }

    public static interface Spiffy extends GetHandle
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@Bind("id") long id, @Bind("name") String name);

        @SqlQuery("select name, id from something")
        @Mapper(SomethingMapper.class)
        Iterator<Something> findAll();
    }

    public static interface TransactionStuff extends GetHandle, Transactional<TransactionStuff>
    {
        @SqlQuery("select id, name from something where id = :id")
        @Mapper(SomethingMapper.class)
        public Something byId(@Bind("id") long id);

        @SqlUpdate("update something set name = :name where id = :id")
        public void updateName(@Bind("id") long id, @Bind("name") String name);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@Bind("id") long id, @Bind("name") String name);
    }

    public static interface ExternalSql extends GetHandle
    {
        @SqlQuery("all-something")
        @Mapper(SomethingMapper.class)
        Iterator<Something> findAll();
    }
}

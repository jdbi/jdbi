/*
 * Copyright (C) 2004 - 2013 Brian McCallister
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
package org.skife.jdbi.v3.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.skife.jdbi.v3.DBI;
import org.skife.jdbi.v3.Handle;
import org.skife.jdbi.v3.Something;
import org.skife.jdbi.v3.sqlobject.Bind;
import org.skife.jdbi.v3.sqlobject.SqlObjectBuilder;
import org.skife.jdbi.v3.sqlobject.SqlQuery;
import org.skife.jdbi.v3.sqlobject.SqlUpdate;
import org.skife.jdbi.v3.sqlobject.customizers.Mapper;
import org.skife.jdbi.v3.sqlobject.mixins.GetHandle;
import org.skife.jdbi.v3.sqlobject.mixins.Transactional;
import org.skife.jdbi.v3.util.StringMapper;

import java.util.Iterator;
import java.util.UUID;

import junit.framework.TestCase;

public class TestOnDemandSqlObject extends TestCase
{
    private DBI    dbi;
    private Handle handle;


    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        // in MVCC mode h2 doesn't shut down immediately on all connections closed, so need random db name
        ds.setURL(String.format("jdbc:h2:mem:%s;MVCC=TRUE", UUID.randomUUID()));
        dbi = new DBI(ds);
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");

    }

    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }


    public void testAPIWorks() throws Exception
    {
        Spiffy s = SqlObjectBuilder.onDemand(dbi, Spiffy.class);

        s.insert(7, "Bill");

        String bill = handle.createQuery("select name from something where id = 7").map(StringMapper.FIRST).first();

        assertEquals("Bill", bill);
    }

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

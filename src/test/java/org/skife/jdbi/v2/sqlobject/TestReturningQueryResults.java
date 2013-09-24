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
package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import junit.framework.TestCase;

public class TestReturningQueryResults extends TestCase
{
    private DBI    dbi;
    private Handle handle;


    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");

    }

    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }

    public void testSingleValue() throws Exception
    {
        handle.execute("insert into something (id, name) values (7, 'Tim')");

        Spiffy spiffy = SqlObjectBuilder.open(dbi, Spiffy.class);


        Something s = spiffy.findById(7);
        assertEquals("Tim", s.getName());
    }

    public void testIterator() throws Exception
    {
        handle.execute("insert into something (id, name) values (7, 'Tim')");
        handle.execute("insert into something (id, name) values (3, 'Diego')");

        Spiffy spiffy = SqlObjectBuilder.open(dbi, Spiffy.class);


        Iterator<Something> itty = spiffy.findByIdRange(2, 10);
        Set<Something> all = new HashSet<Something>();
        while (itty.hasNext()) {
            all.add(itty.next());
        }

        assertEquals(2, all.size());
        assertTrue(all.contains(new Something(7, "Tim")));
        assertTrue(all.contains(new Something(3, "Diego")));
    }


    public void testList() throws Exception
    {
        handle.execute("insert into something (id, name) values (7, 'Tim')");
        handle.execute("insert into something (id, name) values (3, 'Diego')");

        Spiffy spiffy = SqlObjectBuilder.open(dbi, Spiffy.class);


        List<Something> all = spiffy.findTwoByIds(3, 7);

        assertEquals(2, all.size());
        assertTrue(all.contains(new Something(7, "Tim")));
        assertTrue(all.contains(new Something(3, "Diego")));
    }

    public static interface Spiffy extends CloseMe
    {
        @SqlQuery("select id, name from something where id = :id")
        @Mapper(SomethingMapper.class)
        public Something findById(@Bind("id") int id);

        @SqlQuery("select id, name from something where id >= :from and id <= :to")
        @Mapper(SomethingMapper.class)
        public Iterator<Something> findByIdRange(@Bind("from") int from, @Bind("to") int to);

        @SqlQuery("select id, name from something where id = :first or id = :second")
        @Mapper(SomethingMapper.class)
        public List<Something> findTwoByIds(@Bind("first") int from, @Bind("second") int to);

    }
}

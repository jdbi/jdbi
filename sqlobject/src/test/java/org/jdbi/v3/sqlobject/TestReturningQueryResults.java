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
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Something;
import org.jdbi.v3.sqlobject.customizers.Mapper;
import org.jdbi.v3.sqlobject.mixins.CloseMe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestReturningQueryResults
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
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
    public void testSingleValue() throws Exception
    {
        handle.execute("insert into something (id, name) values (7, 'Tim')");

        Spiffy spiffy = SqlObjectBuilder.open(dbi, Spiffy.class);


        Something s = spiffy.findById(7);
        assertEquals("Tim", s.getName());
    }

    @Test
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


    @Test
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

    public interface Spiffy extends CloseMe
    {
        @SqlQuery("select id, name from something where id = :id")
        @Mapper(SomethingMapper.class)
        Something findById(@Bind("id") int id);

        @SqlQuery("select id, name from something where id >= :from and id <= :to")
        @Mapper(SomethingMapper.class)
        Iterator<Something> findByIdRange(@Bind("from") int from, @Bind("to") int to);

        @SqlQuery("select id, name from something where id = :first or id = :second")
        @Mapper(SomethingMapper.class)
        List<Something> findTwoByIds(@Bind("first") int from, @Bind("second") int to);

    }
}

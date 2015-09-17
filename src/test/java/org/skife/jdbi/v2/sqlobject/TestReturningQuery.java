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
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;

import java.util.UUID;

import static org.junit.Assert.assertEquals;


public class TestReturningQuery
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
    public void testWithRegisteredMapper() throws Exception
    {
        handle.execute("insert into something (id, name) values (7, 'Tim')");

        dbi.registerMapper(new SomethingMapper());

        Spiffy spiffy = SqlObjectBuilder.open(dbi, Spiffy.class);

        Something s = spiffy.findById(7)
                            .first();

        assertEquals("Tim", s.getName());
    }

    @Test
    public void testWithExplicitMapper() throws Exception
    {
        handle.execute("insert into something (id, name) values (7, 'Tim')");

        Spiffy2 spiffy = SqlObjectBuilder.open(dbi, Spiffy2.class);

        Something s = spiffy.findByIdWithExplicitMapper(7)
                            .first();

        assertEquals("Tim", s.getName());
    }

    public static interface Spiffy extends CloseMe
    {
        @SqlQuery("select id, name from something where id = :id")
        public Query<Something> findById(@Bind("id") int id);
    }

    public static interface Spiffy2 extends CloseMe
    {
        @SqlQuery("select id, name from something where id = :id")
        @Mapper(SomethingMapper.class)
        public Query<Something> findByIdWithExplicitMapper(@Bind("id") int id);
    }
}

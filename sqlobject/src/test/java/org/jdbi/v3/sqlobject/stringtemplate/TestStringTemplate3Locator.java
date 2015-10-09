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
package org.jdbi.v3.sqlobject.stringtemplate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.UUID;

import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Something;
import org.jdbi.v3.sqlobject.Bind;
import org.jdbi.v3.sqlobject.BindBean;
import org.jdbi.v3.sqlobject.SomethingMapper;
import org.jdbi.v3.sqlobject.SqlBatch;
import org.jdbi.v3.sqlobject.SqlObjectBuilder;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.SqlUpdate;
import org.jdbi.v3.sqlobject.customizers.Define;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestStringTemplate3Locator
{
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:" + UUID.randomUUID());
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
    public void testBaz() throws Exception
    {
        Wombat wombat = SqlObjectBuilder.attach(handle, Wombat.class);
        wombat.insert(new Something(7, "Henning"));

        String name = handle.createQuery("select name from something where id = 7")
                            .mapTo(String.class)
                            .first();

        assertThat(name, equalTo("Henning"));
    }

    @Test
    public void testBam() throws Exception
    {
        handle.execute("insert into something (id, name) values (6, 'Martin')");

        Something s = SqlObjectBuilder.attach(handle, Wombat.class).findById(6L);
        assertThat(s.getName(), equalTo("Martin"));
    }

    @Test
    public void testBap() throws Exception
    {
        handle.execute("insert into something (id, name) values (2, 'Bean')");
        Wombat w = SqlObjectBuilder.attach(handle, Wombat.class);
        assertThat(w.findNameFor(2), equalTo("Bean"));
    }

    @Test
    public void testDefines() throws Exception
    {
        SqlObjectBuilder.attach(handle, Wombat.class).weirdInsert("something", "id", "name", 5, "Bouncer");
        SqlObjectBuilder.attach(handle, Wombat.class).weirdInsert("something", "id", "name", 6, "Bean");
        String name = handle.createQuery("select name from something where id = 5")
                            .mapTo(String.class)
                            .first();

        assertThat(name, equalTo("Bouncer"));
    }


    @Test
    public void testBatching() throws Exception
    {
        Wombat roo = SqlObjectBuilder.attach(handle, Wombat.class);
        roo.insertBunches(new Something(1, "Jeff"), new Something(2, "Brian"));

        assertThat(roo.findById(1L), equalTo(new Something(1, "Jeff")));
        assertThat(roo.findById(2L), equalTo(new Something(2, "Brian")));
    }

    @Test
    public void testNoTemplateDefined() throws Exception
    {
        HoneyBadger badass = SqlObjectBuilder.attach(handle, HoneyBadger.class);

        badass.insert("something", new Something(1, "Ted"));
        badass.insert("something", new Something(2, "Fred"));
    }

    @UseStringTemplate3StatementLocator
    @RegisterMapper(SomethingMapper.class)
    static interface HoneyBadger
    {
        @SqlUpdate("insert into <table> (id, name) values (:id, :name)")
        public void insert(@Define("table") String table, @BindBean Something s);

        @SqlQuery("select id, name from <table> where id = :id")
        public Something findById(@Define("table") String table, @Bind("id") Long id);
    }

    @UseStringTemplate3StatementLocator
    @RegisterMapper(SomethingMapper.class)
    static interface Wombat
    {
        @SqlUpdate
        public void insert(@BindBean Something s);

        @SqlQuery
        public Something findById(@Bind("id") Long id);

        @SqlQuery("select name from something where id = :id")
        String findNameFor(@Bind("id") int id);

        @SqlUpdate
        void weirdInsert(@Define("table") String table,
                         @Define("id_column") String idColumn,
                         @Define("value_column") String valueColumn,
                         @Bind("id") int id,
                         @Bind("value") String name);

        @SqlBatch
        void insertBunches(@BindBean Something... somethings);
    }
}

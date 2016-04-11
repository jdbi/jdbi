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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.customizers.Define;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.stringtemplate.ExternalizedSqlViaStringTemplate3;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

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
        Wombat wombat = handle.attach(Wombat.class);
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

        Something s = handle.attach(Wombat.class).findById(6L);
        assertThat(s.getName(), equalTo("Martin"));
    }

    @Test
    public void testBap() throws Exception
    {
        handle.execute("insert into something (id, name) values (2, 'Bean')");
        Wombat w = handle.attach(Wombat.class);
        assertThat(w.findNameFor(2), equalTo("Bean"));
    }

    @Test
    public void testDefines() throws Exception
    {
        handle.attach(Wombat.class).weirdInsert("something", "id", "name", 5, "Bouncer");
        handle.attach(Wombat.class).weirdInsert("something", "id", "name", 6, "Bean");
        String name = handle.createQuery("select name from something where id = 5")
                            .mapTo(String.class)
                            .first();

        assertThat(name, equalTo("Bouncer"));
    }


    @Test
    public void testBatching() throws Exception
    {
        Wombat roo = handle.attach(Wombat.class);
        roo.insertBunches(new Something(1, "Jeff"), new Something(2, "Brian"));

        assertThat(roo.findById(1L), equalTo(new Something(1, "Jeff")));
        assertThat(roo.findById(2L), equalTo(new Something(2, "Brian")));
    }

    @Test
    public void testNoTemplateDefined() throws Exception
    {
        HoneyBadger badass = handle.attach(HoneyBadger.class);

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

    @ExternalizedSqlViaStringTemplate3
    @RegisterMapper(SomethingMapper.class)
    static interface Wombat
    {
        @SqlUpdate
        public void insert(@BindBean Something s);

        @SqlQuery
        public Something findById(@Bind("id") Long id);

        @SqlQuery("select name from something where id = :it")
        String findNameFor(@Bind int id);

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

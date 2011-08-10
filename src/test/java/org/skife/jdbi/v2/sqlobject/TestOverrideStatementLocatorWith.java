/*
 * Copyright 2004 - 2011 Brian McCallister
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

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.customizers.Define;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterArgumentFactory;
import org.skife.jdbi.v2.sqlobject.customizers.OverrideStatementLocatorWith;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.stringtemplate.StringTemplate3StatementLocator;
import org.skife.jdbi.v2.util.StringMapper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestOverrideStatementLocatorWith
{
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        DBI dbi = new DBI(ds);
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
        Kangaroo wombat = handle.attach(Kangaroo.class);
        wombat.insert(new Something(7, "Henning"));

        String name = handle.createQuery("select name from something where id = 7")
                            .map(StringMapper.FIRST)
                            .first();

        assertThat(name, equalTo("Henning"));
    }

    @Test
    public void testBam() throws Exception
    {
        handle.execute("insert into something (id, name) values (6, 'Martin')");

        Something s = handle.attach(Kangaroo.class).findById(6L);
        assertThat(s.getName(), equalTo("Martin"));
    }

    @Test
    public void testBap() throws Exception
    {
        handle.execute("insert into something (id, name) values (2, 'Bean')");
        Kangaroo w = handle.attach(Kangaroo.class);
        assertThat(w.findNameFor(2), equalTo("Bean"));
    }

    @Test
    public void testDefines() throws Exception
    {
        handle.attach(Kangaroo.class).weirdInsert("something", "id", "name", 5, "Bouncer");
        String name = handle.createQuery("select name from something where id = 5")
                            .map(StringMapper.FIRST)
                            .first();

        assertThat(name, equalTo("Bouncer"));
    }


    @OverrideStatementLocatorWith(StringTemplate3StatementLocator.class)
    @RegisterMapper(SomethingMapper.class)
    static interface Kangaroo
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
    }

}

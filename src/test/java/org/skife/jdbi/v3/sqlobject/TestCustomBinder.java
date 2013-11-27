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
import org.skife.jdbi.v3.sqlobject.mixins.CloseMe;

import java.util.UUID;

import junit.framework.TestCase;

public class TestCustomBinder extends TestCase
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

    public void testFoo() throws Exception
    {
        handle.execute("insert into something (id, name) values (2, 'Martin')");
        Spiffy spiffy = SqlObjectBuilder.open(dbi, Spiffy.class);

        Something s = spiffy.findSame(new Something(2, "Unknown"));

        assertEquals("Martin", s.getName());

        spiffy.close();
    }

    public void testCustomBindingAnnotation() throws Exception
    {
        Spiffy s = handle.attach(Spiffy.class);

        s.insert(new Something(2, "Keith"));

        assertEquals("Keith", s.findNameById(2));
    }


    public static interface Spiffy extends CloseMe
    {
        @SqlQuery("select id, name from something where id = :it.id")
        @Mapper(SomethingMapper.class)
        public Something findSame(@Bind(value = "it", binder = SomethingBinderAgainstBind.class) Something something);

        @SqlUpdate("insert into something (id, name) values (:s.id, :s.name)")
        public int insert(@BindSomething("s") Something something);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int i);
    }
}

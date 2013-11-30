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
package org.jdbi.v3.sqlobject;

import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;

import junit.framework.TestCase;

public class TestEnumMapping extends TestCase
{
    private DBI    dbi;
    private Handle handle;

    @Override
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        handle = dbi.open();

        handle.execute("create table something (id int primary key auto_increment, name varchar(10))");
    }

    @Override
    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }

    public void testEnums() throws Exception
    {
        dbi.registerMapper(new SomethingMapper());

        Spiffy spiffy = SqlObjectBuilder.open(dbi, Spiffy.class);

        int bobId = spiffy.addCoolName(CoolName.BOB);
        int joeId = spiffy.addCoolName(CoolName.JOE);

        assertSame(CoolName.BOB, spiffy.findById(bobId));
        assertSame(CoolName.JOE, spiffy.findById(joeId));
    }

    public static interface Spiffy
    {
        @SqlUpdate("insert into something(name) values(:name)")
        @GetGeneratedKeys
        int addCoolName(@Bind("name") CoolName coolName);

        @SqlQuery("select name from something where id = :id")
        CoolName findById(@Bind("id") int id);
    }

    public enum CoolName
    {
        BOB, FRANK, JOE
    }
}

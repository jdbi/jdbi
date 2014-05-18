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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestObjectMethods
{

    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        dbi.registerMapper(new SomethingMapper());
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
    public void testToString() throws Exception
    {
        DAO dao = SqlObjectBuilder.attach(handle, DAO.class);
        assertThat(dao.toString(), containsString(DAO.class.getName()));
    }

    @Test
    public void testEquals() throws Exception
    {
        DAO dao = SqlObjectBuilder.attach(handle, DAO.class);
        assertThat(dao, equalTo(dao));
    }

    @Test
    public void testNotEquals() throws Exception
    {
        DAO dao = SqlObjectBuilder.attach(handle, DAO.class);
        DAO oad = SqlObjectBuilder.attach(handle, DAO.class);
        assertThat(dao, not(equalTo(oad)));
    }

    @Test
    public void testHashCodeDiff() throws Exception
    {
        DAO dao = SqlObjectBuilder.attach(handle, DAO.class);
        DAO oad = SqlObjectBuilder.attach(handle, DAO.class);
        assertThat(dao.hashCode(), not(equalTo(oad.hashCode())));
    }

    @Test
    public void testHashCodeMatch() throws Exception
    {
        DAO dao = SqlObjectBuilder.attach(handle, DAO.class);
        assertThat(dao.hashCode(), equalTo(dao.hashCode()));
    }


    public static interface DAO
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@Bind("id")long id, @Bind("name") String name);
    }
}

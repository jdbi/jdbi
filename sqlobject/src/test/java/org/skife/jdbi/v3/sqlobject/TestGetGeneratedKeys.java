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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.UUID;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v3.DBI;
import org.skife.jdbi.v3.Handle;
import org.skife.jdbi.v3.sqlobject.mixins.CloseMe;
import org.skife.jdbi.v3.tweak.HandleCallback;

public class TestGetGeneratedKeys
{
    private JdbcConnectionPool ds;
    private DBI                dbi;

    @Before
    public void setUp() throws Exception
    {
        ds = JdbcConnectionPool.create("jdbc:h2:mem:" + UUID.randomUUID(),
                                       "username",
                                       "password");
        dbi = new DBI(ds);
        dbi.withHandle(new HandleCallback<Object>()
        {
            @Override
            public Object withHandle(Handle handle) throws Exception
            {
                handle.execute("create table something (id identity primary key, name varchar(32))");
                return null;
            }
        });
    }

    @After
    public void tearDown() throws Exception
    {
        ds.dispose();
    }

    public static interface DAO extends CloseMe
    {
        @SqlUpdate("insert into something (name) values (:it)")
        @GetGeneratedKeys
        public long insert(@Bind String name);

        @SqlQuery("select name from something where id = :it")
        public String findNameById(@Bind long id);
    }

    @Test
    public void testFoo() throws Exception
    {
        DAO dao = SqlObjectBuilder.open(dbi, DAO.class);

        long brian_id = dao.insert("Brian");
        long keith_id = dao.insert("Keith");

        assertThat(dao.findNameById(brian_id), equalTo("Brian"));
        assertThat(dao.findNameById(keith_id), equalTo("Keith"));

        dao.close();
    }

}

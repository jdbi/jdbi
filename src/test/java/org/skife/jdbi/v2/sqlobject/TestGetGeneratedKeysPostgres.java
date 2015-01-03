/*
 * Copyright (C) 2004 - 2014 Brian McCallister
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

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;
import org.skife.jdbi.v2.tweak.HandleCallback;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestGetGeneratedKeysPostgres
{
    private JdbcConnectionPool ds;
    private DBI                dbi;

    @Before
    public void setUp() throws Exception {
        dbi = new DBI("jdbc:postgresql:test", "postgres", "postgres");
        dbi.withHandle(new HandleCallback<Object>() {
            @Override
            public Object withHandle(Handle handle) throws Exception
            {
                handle.execute("create sequence id_sequence INCREMENT 1 START WITH 100");
                handle.execute("create table if not exists something (name text, id int DEFAULT nextval('id_sequence'), CONSTRAINT something_id PRIMARY KEY ( id ));");
                return null;
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        dbi.withHandle(new HandleCallback<Object>() {
            @Override
            public Object withHandle(Handle handle) throws Exception
            {
                handle.execute("drop table something");
                handle.execute("drop sequence id_sequence");
                return null;
            }
        });
    }

    public static interface DAO extends CloseMe {
        @SqlUpdate("insert into something (name, id) values (:name, nextval('id_sequence'))")
        @GetGeneratedKeys(columnName = "id")
        public long insert(@Bind("name") String name);

        @SqlQuery("select name from something where id = :it")
        public String findNameById(@Bind long id);
    }

    @Ignore
    @Test
    public void testFoo() throws Exception {
        DAO dao = dbi.open(DAO.class);

        Long brian_id = dao.insert("Brian");
        long keith_id = dao.insert("Keith");

        assertThat(dao.findNameById(brian_id), equalTo("Brian"));
        assertThat(dao.findNameById(keith_id), equalTo("Keith"));

        dao.close();
    }
}

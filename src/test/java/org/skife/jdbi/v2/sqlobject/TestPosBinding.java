/*
 * Copyright (C) 2004 - 2015 Brian McCallister
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

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Tests for positional binding using {@link Pos} annotation.
 */
public class TestPosBinding {

    private DBI dbi;

    private Handle handle;

    private SomethingDao somethingDao;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        handle = dbi.open();
        somethingDao = handle.attach(SomethingDao.class);

        handle.execute("create table something (id int primary key, name varchar(100), code int)");
        handle.execute("insert into something values (7, 'Tim', 12)");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }

    @Test
    public void testQueryWithRevertedParams()
    {
        Integer id = somethingDao.getIdByNameAndCode("Tim", 12);
        assertEquals(7, id.intValue());
    }

    @Test
    public void testUpdate()
    {
        int id = 2;
        String name = "Diego";
        int code = 90;
        somethingDao.insertIntoSomething(id, name, code);
        List<Map<String, Object>> rows = handle.select("select * from something where id = ?", id);
        assertEquals(rows.size(), 1);

        Map<String, Object> row = rows.get(0);
        assertEquals(row.get("id"), id);
        assertEquals(row.get("name"), name);
        assertEquals(row.get("code"), code);
    }

    interface SomethingDao
    {
        @SqlQuery("select id from something where code=? and name=?")
        Integer getIdByNameAndCode(@Pos(1) String name, @Pos(0) int code);

        @SqlUpdate("insert into something values (?, ?, ?)")
        void insertIntoSomething(@Pos int id, @Pos(1) String name, @Pos(2) int code);
    }
}

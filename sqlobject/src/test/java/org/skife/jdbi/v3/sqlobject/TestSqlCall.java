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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v3.DBI;
import org.skife.jdbi.v3.Handle;
import org.skife.jdbi.v3.Something;
import org.skife.jdbi.v3.logging.PrintStreamLog;
import org.skife.jdbi.v3.sqlobject.customizers.RegisterMapper;

public class TestSqlCall
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:" + UUID.randomUUID());

        dbi.setSQLLog(new PrintStreamLog(System.out));
        handle = dbi.open();
        handle.execute("create table something( id integer primary key, name varchar(100) )");
        handle.execute("CREATE ALIAS stored_insert FOR \"org.skife.jdbi.v3.sqlobject.TestSqlCall.insertSomething\";");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.close();
    }

    @Test
    public void testFoo() throws Exception
    {
        Dao dao = SqlObjectBuilder.attach(handle, Dao.class);
//        OutParameters out = handle.createCall(":num = call stored_insert(:id, :name)")
//                                  .bind("id", 1)
//                                  .bind("name", "Jeff")
//                                  .registerOutParameter("num", Types.INTEGER)
//                                  .invoke();
        dao.insert(1, "Jeff");

        assertThat(SqlObjectBuilder.attach(handle, Dao.class).findById(1), equalTo(new Something(1, "Jeff")));
    }

    public static interface Dao
    {
        @SqlCall("call stored_insert(:id, :name)")
        public void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        @RegisterMapper(SomethingMapper.class)
        Something findById(@Bind("id") int id);
    }


    public static int insertSomething(Connection conn, int id, String name) throws SQLException
    {

        PreparedStatement stmt = conn.prepareStatement("insert into something (id, name) values (?, ?)");
        stmt.setInt(1, id);
        stmt.setString(2, name);
        return stmt.executeUpdate();
    }
}

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
package org.jdbi.v3.sqlobject;


import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizers.RegisterRowMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestSqlCall
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        handle = db.getSharedHandle();
        handle.execute("CREATE ALIAS stored_insert FOR \"org.jdbi.v3.sqlobject.TestSqlCall.insertSomething\";");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.close();
    }

    @Test
    public void testFoo() throws Exception
    {
        Dao dao = handle.attach(Dao.class);
//        OutParameters out = handle.createCall(":num = call stored_insert(:id, :name)")
//                                  .bind("id", 1)
//                                  .bind("name", "Jeff")
//                                  .registerOutParameter("num", Types.INTEGER)
//                                  .invoke();
        dao.insert(1, "Jeff");

        assertThat(handle.attach(Dao.class).findById(1)).isEqualTo(new Something(1, "Jeff"));
    }

    public interface Dao
    {
        @SqlCall("call stored_insert(:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        @RegisterRowMapper(SomethingMapper.class)
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

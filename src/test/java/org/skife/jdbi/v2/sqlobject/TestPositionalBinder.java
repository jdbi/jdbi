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

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.logging.PrintStreamLog;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class TestPositionalBinder {

    private Handle handle;
    private SomethingDao somethingDao;

    @Before
    public void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        DBI dbi = new DBI(ds);
        dbi.setSQLLog(new PrintStreamLog());
        handle = dbi.open();
        somethingDao = handle.attach(SomethingDao.class);

        handle.execute("create table something (something_id int primary key, name varchar(100), code int)");
        handle.execute("insert into something(something_id, name, code) values (1, 'Brian', 12)");
        handle.execute("insert into something(something_id, name, code) values (2, 'Keith', 27)");
        handle.execute("insert into something(something_id, name, code) values (3, 'Coda', 14)");
    }

    @After
    public void tearDown() throws Exception {
        handle.close();
    }

    @Test
    public void testOnePositionalParameter() {
        String name = somethingDao.findNameById(2);
        assertEquals("Keith", name);
    }

    @Test
    public void testManyPositionalParameters() {
        Integer id = somethingDao.getIdByNameAndCode("Coda", 14);
        assertEquals(3, id.intValue());
    }

    @Test
    public void testInsertWithPositionalParameters() {
        somethingDao.insertSomething(4, "Dave", 90);

        List<Map<String, Object>> rows = handle.select("select * from something where something_id=?", 4);
        assertEquals(rows.size(), 1);

        Map<String, Object> row = rows.get(0);
        assertEquals(row.get("something_id"), 4);
        assertEquals(row.get("name"), "Dave");
        assertEquals(row.get("code"), 90);
    }

    @Test
    public void testInsertWithDefaultParams(){
        somethingDao.insertWithDefaultParams("Greg",21);
        List<Map<String, Object>> rows = handle.select("select * from something where something_id=?", 19);
        assertEquals(rows.size(), 1);

        Map<String, Object> row = rows.get(0);
        assertEquals(row.get("something_id"), 19);
        assertEquals(row.get("name"), "Greg");
        assertEquals(row.get("code"), 21);
    }

    static interface SomethingDao {

        @SqlQuery("select name from something where something_id=:0")
        String findNameById(int i);

        @SqlQuery("select something_id from something where name=:0 and code=:1")
        Integer getIdByNameAndCode(String name, int code);

        @SqlUpdate("insert into something(something_id, name, code) values (:0, :1, :2)")
        void insertSomething(int id, String name, int code);

        @SqlUpdate("insert into something(something_id,name, code) values (19, :0, :code)")
        void insertWithDefaultParams(String name, @Bind("code") int code);
    }
}

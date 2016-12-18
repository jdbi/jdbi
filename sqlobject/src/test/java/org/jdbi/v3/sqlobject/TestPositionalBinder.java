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

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestPositionalBinder {

    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;
    private SomethingDao somethingDao;

    @Before
    public void setUp() throws Exception {
        handle = db.getSharedHandle();
        somethingDao = handle.attach(SomethingDao.class);

        handle.execute("drop table something");
        handle.execute("create table something (something_id int primary key, name varchar(100), code int)");
        handle.execute("insert into something(something_id, name, code) values (1, 'Brian', 12)");
        handle.execute("insert into something(something_id, name, code) values (2, 'Keith', 27)");
        handle.execute("insert into something(something_id, name, code) values (3, 'Coda', 14)");
    }

    @Test
    public void testOnePositionalParameter() {
        String name = somethingDao.findNameById(2);
        assertThat(name).isEqualTo("Keith");
    }

    @Test
    public void testManyPositionalParameters() {
        Integer id = somethingDao.getIdByNameAndCode("Coda", 14);
        assertThat(id).isEqualTo(3);
    }

    @Test
    public void testInsertWithPositionalParameters() {
        somethingDao.insertSomething(4, "Dave", 90);

        List<Map<String, Object>> rows = handle.select("select * from something where something_id=?", 4).mapToMap().list();
        assertThat(rows).containsExactlyElementsOf(ImmutableList.of(
                ImmutableMap.of("something_id", 4, "name", "Dave", "code", 90)));
    }

    @Test
    public void testInsertWithDefaultParams(){
        somethingDao.insertWithDefaultParams("Greg",21);
        List<Map<String, Object>> rows = handle.select("select * from something where something_id=?", 19).mapToMap().list();
        assertThat(rows).containsExactlyElementsOf(ImmutableList.of(
                ImmutableMap.of("something_id", 19, "name", "Greg", "code", 21)));
    }

    public interface SomethingDao {

        @SqlQuery("select name from something where something_id=:0")
        String findNameById(int i);

        @SqlQuery("select something_id from something where name=:0 and code=:1")
        Integer getIdByNameAndCode(String name, int code);

        @SqlUpdate("insert into something(something_id, name, code) values (:0, :1, :2)")
        void insertSomething(int id, @Bind String name, int code);

        @SqlUpdate("insert into something(something_id,name, code) values (19, :0, :code)")
        void insertWithDefaultParams(String name, @Bind("code") int code);
    }
}

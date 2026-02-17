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
package org.jdbi.sqlobject;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jdbi.core.Handle;
import org.jdbi.core.statement.UnableToExecuteStatementException;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestPositionalBinder {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    private Handle handle;
    private SomethingDao somethingDao;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
        somethingDao = handle.attach(SomethingDao.class);

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
    public void testInsertWithDefaultParams() {
        somethingDao.insertWithDefaultParams("Greg", 21);
        List<Map<String, Object>> rows = handle.select("select * from something where something_id=?", 19).mapToMap().list();
        assertThat(rows).containsExactlyElementsOf(ImmutableList.of(
                ImmutableMap.of("something_id", 19, "name", "Greg", "code", 21)));
    }

    @Test
    public void testInsertWithMixedPositionalAndNamedParams() {
        assertThatThrownBy(() ->
                somethingDao.insertWithMixedPositionalAndNamedParams("Jenny", 867_5309))
                .isInstanceOf(UnableToExecuteStatementException.class)
                .hasMessageContaining("Cannot mix named and positional parameters in a SQL statement");
    }

    public interface SomethingDao {

        @SqlQuery("select name from something where something_id=?")
        String findNameById(int i);

        @SqlQuery("select something_id from something where name=? and code=?")
        Integer getIdByNameAndCode(String name, int code);

        @SqlUpdate("insert into something(something_id, name, code) values (?, ?, ?)")
        void insertSomething(int id, String name, int code);

        @SqlUpdate("insert into something(something_id,name, code) values (19, ?, ?)")
        void insertWithDefaultParams(String name, int code);

        @SqlUpdate("insert into something(something_id,name,code) values (19, :name, ?)")
        void insertWithMixedPositionalAndNamedParams(String name, int code);
    }
}

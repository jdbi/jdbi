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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.jdbi.v3.core.PgDatabaseRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestGetGeneratedKeysPostgres
{
    @Rule
    public PgDatabaseRule db = new PgDatabaseRule().withPlugin(new SqlObjectPlugin());

    @Before
    public void setUp() throws Exception {
        db.getJdbi().useHandle(handle -> {
            handle.execute("create sequence id_sequence INCREMENT 1 START WITH 100");
            handle.execute("create table if not exists something (name text, id int DEFAULT nextval('id_sequence'), CONSTRAINT something_id PRIMARY KEY ( id ));");
        });
    }

    @After
    public void tearDown() throws Exception {
        db.getJdbi().useHandle(handle -> {
            handle.execute("drop table something");
            handle.execute("drop sequence id_sequence");
        });
    }

    public interface DAO {
        @SqlUpdate("insert into something (name, id) values (:name, nextval('id_sequence'))")
        @GetGeneratedKeys(columnName = "id")
        long insert(@Bind("name") String name);

        @SqlBatch("insert into something (name) values (:name)")
        @GetGeneratedKeys(columnName = "id")
        int[] insert(@Bind("name") List<String> names);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind long id);
    }

    @Test
    public void testFoo() throws Exception {
        db.getJdbi().useExtension(DAO.class, dao -> {
            long brian_id = dao.insert("Brian");
            long keith_id = dao.insert("Keith");

            assertThat(dao.findNameById(brian_id), equalTo("Brian"));
            assertThat(dao.findNameById(keith_id), equalTo("Keith"));
        });
    }

    @Test
    public void testBatch() throws Exception {
        db.getJdbi().useExtension(DAO.class, dao -> {
            int[] ids = dao.insert(Arrays.asList("Burt", "Macklin"));
            assertThat(dao.findNameById(ids[0]), equalTo("Burt"));
            assertThat(dao.findNameById(ids[1]), equalTo("Macklin"));
        });
    }
}

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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestGetGeneratedKeysHsqlDb {

    private Jdbi db;

    @Before
    public void setUp() {
        db = Jdbi.create("jdbc:hsqldb:mem:" + UUID.randomUUID(), "username", "password")
                .installPlugin(new SqlObjectPlugin());
        db.useHandle(handle -> handle.execute("create table something (id identity primary key, name varchar(32))"));
    }

    public interface DAO {
        @SqlUpdate("insert into something (name) values (:name)")
        @GetGeneratedKeys
        long insert(String name);

        @SqlBatch("insert into something (name) values(:names)")
        @GetGeneratedKeys
        int[] insert(List<String> names);

        @SqlQuery("select name from something where id = :id")
        String findNameById(long id);
    }

    @Test
    public void testFoo() {
        db.useExtension(DAO.class, dao -> {
            long brianId = dao.insert("Brian");
            long keithId = dao.insert("Keith");

            assertThat(dao.findNameById(brianId)).isEqualTo("Brian");
            assertThat(dao.findNameById(keithId)).isEqualTo("Keith");
        });
    }

    @Test
    public void testBatch() {
        db.useExtension(DAO.class, dao -> {
            int[] ids = dao.insert(Arrays.asList("Burt", "Macklin"));
            assertThat(dao.findNameById(ids[0])).isEqualTo("Burt");
            assertThat(dao.findNameById(ids[1])).isEqualTo("Macklin");
        });
    }
}

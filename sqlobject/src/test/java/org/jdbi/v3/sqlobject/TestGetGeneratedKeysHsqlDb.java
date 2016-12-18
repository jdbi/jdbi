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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Test;

public class TestGetGeneratedKeysHsqlDb {

    private Jdbi dbi;

    @Before
    public void setUp() throws Exception {
        dbi = Jdbi.create("jdbc:hsqldb:mem:" + UUID.randomUUID(), "username", "password")
                .installPlugin(new SqlObjectPlugin());
        dbi.useHandle(handle -> handle.execute("create table something (id identity primary key, name varchar(32))"));
    }

    public interface DAO {
        @SqlUpdate("insert into something (name) values (:name)")
        @GetGeneratedKeys
        long insert(@Bind String name);

        @SqlBatch("insert into something (name) values(:name)")
        @GetGeneratedKeys
        int[] insert(@Bind List<String> names);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind long id);
    }

    @Test
    public void testFoo() throws Exception {
        dbi.useExtension(DAO.class, dao -> {
            long brian_id = dao.insert("Brian");
            long keith_id = dao.insert("Keith");

            assertThat(dao.findNameById(brian_id)).isEqualTo("Brian");
            assertThat(dao.findNameById(keith_id)).isEqualTo("Keith");
        });
    }

    @Test
    public void testBatch() throws Exception {
        dbi.useExtension(DAO.class, dao -> {
            int[] ids = dao.insert(Arrays.asList("Burt", "Macklin"));
            assertThat(dao.findNameById(ids[0])).isEqualTo("Burt");
            assertThat(dao.findNameById(ids[1])).isEqualTo("Macklin");
        });
    }
}

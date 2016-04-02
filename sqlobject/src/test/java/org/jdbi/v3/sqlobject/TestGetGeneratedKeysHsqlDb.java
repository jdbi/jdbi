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

import java.util.UUID;

import org.jdbi.v3.DBI;
import org.jdbi.v3.sqlobject.mixins.CloseMe;
import org.junit.Before;
import org.junit.Test;

public class TestGetGeneratedKeysHsqlDb {

    private DBI dbi;

    @Before
    public void setUp() throws Exception {
        dbi = DBI.create("jdbc:hsqldb:mem:" + UUID.randomUUID(), "username", "password")
                .installPlugin(new SqlObjectPlugin());
        dbi.useHandle(handle -> handle.execute("create table something (id identity primary key, name varchar(32))"));
    }

    public interface DAO extends CloseMe {
        @SqlUpdate("insert into something (name) values (:name)")
        @GetGeneratedKeys
        long insert(@Bind String name);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind long id);
    }

    @Test
    public void testFoo() throws Exception {
        dbi.useExtension(DAO.class, dao -> {
            long brian_id = dao.insert("Brian");
            long keith_id = dao.insert("Keith");

            assertThat(dao.findNameById(brian_id), equalTo("Brian"));
            assertThat(dao.findNameById(keith_id), equalTo("Keith"));
        });
    }

}

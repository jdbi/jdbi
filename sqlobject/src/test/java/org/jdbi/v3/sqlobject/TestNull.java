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

import org.jdbi.v3.core.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class TestNull {

    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private DAO dao;

    @Before
    public void setUp() throws Exception {
        dao = db.getSharedHandle().attach(DAO.class);
        dao.insert(1, "brian");
        dao.insert(2, null);
    }
    @Test
    public void testNotNullResult() {
        assertThat(dao.findNameById(1), equalTo("brian"));
    }

    @Test
    public void testNullResult() {
        assertThat(dao.findNameById(2), nullValue());
    }

    @Test
    public void testNoResult() {
        assertThat(dao.findNameById(3), nullValue());
    }

    interface DAO {

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") long id, @Bind("name") String name);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") long id);
    }
}

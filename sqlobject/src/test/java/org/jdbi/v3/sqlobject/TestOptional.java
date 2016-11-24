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
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizers.RegisterRowMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;

public class TestOptional {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private DAO dao;

    @Before
    public void setUp() throws Exception {
        dao = db.getSharedHandle().attach(DAO.class);
        dao.insert(1, "brian");
        dao.insert(2, "eric");
    }

    @Test
    public void testOptionalParameterPresent() {
        assertThat(dao.findIds(Optional.of("brian"))).containsExactly(1);
    }

    @Test
    public void testOptionalPresentAbsent() {
        assertThat(dao.findIds(Optional.empty())).containsExactly(1, 2);
    }

    @Test
    public void testOptionalReturnPresent() {
        assertThat(dao.findNameById(1)).contains("brian");
    }

    @Test
    public void testOptionalReturnAbsent() {
        assertThat(dao.findNameById(3)).isEmpty();
    }

    @Test
    public void testNullReturnsAbsent() {
        dao.insert(3, null);
        assertThat(dao.findNameById(3)).isEmpty();
    }

    @Test(expected = IllegalStateException.class)
    public void testOptionalReturnMultiple() {
        dao.findMultiple();
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface DAO {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") long id, @Bind("name") String name);

        @SqlQuery("select id from something where :name is null or name = :name order by id")
        List<Integer> findIds(@Bind("name") Optional<String> name);

        @SqlQuery("select name from something where id = :id")
        Optional<String> findNameById(@Bind long id);

        @SqlQuery("select name from something")
        Optional<String> findMultiple();
    }
}

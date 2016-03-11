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

import com.google.common.collect.ImmutableList;
import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestOptionalParameters {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private DAO dao;

    @Before
    public void setUp() throws Exception {
        dao = SqlObjects.attach(db.getSharedHandle(), DAO.class);
        dao.insert(1, "brian");
        dao.insert(2, "eric");
    }

    @Test
    public void testOptionalPresent() {
        assertThat(dao.findIds(Optional.of("brian")), equalTo(ImmutableList.of(1)));
    }

    @Test
    public void testOptionalAbsent() {
        assertThat(dao.findIds(Optional.empty()), equalTo(ImmutableList.of(1, 2)));
    }

    @RegisterMapper(SomethingMapper.class)
    public interface DAO {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") long id, @Bind("name") String name);

        @SqlQuery("select id from something where :name is null or name = :name order by id")
        List<Integer> findIds(@Bind("name") Optional<String> name);
    }
}

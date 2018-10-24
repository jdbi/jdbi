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

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestRegisterConstructorMapper {

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Dao dao;

    @Before
    public void setUp() {
        dao = dbRule.getSharedHandle().attach(Dao.class);
    }

    @Test
    public void testMapperRegistered() {
        dao.insert(1, "brain");
        Something brain = dao.getById(1);
        assertThat(brain.getId()).isEqualTo(1);
        assertThat(brain.getName()).isEqualTo("brain");
    }

    @Test
    public void testMapperPrefixed() {
        dao.insert(1, "brain");
        Something brain = dao.getByIdPrefixed(1);
        assertThat(brain.getId()).isEqualTo(1);
        assertThat(brain.getName()).isEqualTo("brain");
    }

    // subclassing just to hide Something() constructor
    public static class SubSomething extends Something {
        public SubSomething(int id, String name) {
            super(id, name);
        }
    }

    public interface Dao {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id=:id")
        @RegisterConstructorMapper(SubSomething.class)
        SubSomething getById(@Bind("id") int id);

        @SqlQuery("select id thing_id, name thing_name from something where id=:id")
        @RegisterConstructorMapper(value = SubSomething.class, prefix = "thing_")
        SubSomething getByIdPrefixed(@Bind("id") int id);
    }
}

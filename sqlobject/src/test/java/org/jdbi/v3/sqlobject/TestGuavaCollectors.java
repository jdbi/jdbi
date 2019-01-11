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
import lombok.Data;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.guava.GuavaPlugin;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

public class TestGuavaCollectors {

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin()).withPlugin(new GuavaPlugin());

    Handle h;

    @Before
    public void setUp() {
        h = dbRule.getSharedHandle();
        h.execute("create table users (id int, name varchar)");
        h.execute("insert into users (id, name) values (?, ?)", 1, "Alice");
        h.execute("insert into users (id, name) values (?, ?)", 2, "Bob");
    }

    @Test
    public void testImmutableList() {
        assertThat(h.attach(UserDao.class).list())
                .containsExactly(new User(1, "Alice"),
                                 new User(2, "Bob"));
    }

    @Test
    public void testOptional() {
        assertThat(h.attach(UserDao.class).getById(1))
                .contains(new User(1, "Alice"));
    }

    @RegisterConstructorMapper(User.class)
    // tag::returnTypes[]
    public interface UserDao {
        @SqlQuery("select * from users order by name")
        ImmutableList<User> list();

        @SqlQuery("select * from users where id = :id")
        com.google.common.base.Optional<User> getById(long id);
    }
    // end::returnTypes[]

    @Data
    public static class User {
        private final int id;
        private final String name;
    }
}

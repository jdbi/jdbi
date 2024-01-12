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
package org.jdbi.v3.e2e.java17;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class TestTextBlock {

    @RegisterExtension
    JdbiExtension h2Extension = JdbiExtension.h2()
            .withPlugin(new SqlObjectPlugin())
            .withInitializer(TestingInitializers.usersWithData())
            .withConfig(RowMappers.class, r -> r.register(User.class, ConstructorMapper.of(User.class)));

    UserDao dao;

    @BeforeEach
    void setUp() {
        this.dao = h2Extension.getSharedHandle().attach(UserDao.class);
    }

    @Test
    void testMapping() {
        var users = dao.getUsers();
        assertThat(users).hasSize(2)
                .containsExactly(new User(1, "Alice"), new User(2, "Bob"));
    }

    @Test
    void testBinding() {
        dao.insertUser(new User(3, "Charlie"));

        var user = dao.getUser(3);
        assertThat(user).isPresent()
                .contains(new User(3, "Charlie"));
    }

    public record User(int id, String name) {}

    interface UserDao extends SqlObject {

        @SqlQuery("""
                SELECT *
                FROM users
                ORDER BY id
                """)
        List<User> getUsers();

        @SqlQuery("""
                SELECT *
                FROM users
                WHERE id = :id ORDER BY id
                """)
        Optional<User> getUser(int id);

        @SqlUpdate("""
                INSERT INTO users (id, name)
                VALUES (:id, :name)
                """)
        void insertUser(@BindMethods User user);
    }
}

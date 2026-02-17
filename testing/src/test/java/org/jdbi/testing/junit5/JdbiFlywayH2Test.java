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
package org.jdbi.testing.junit5;

import java.util.List;
import java.util.Objects;

import org.jdbi.core.Jdbi;
import org.jdbi.core.mapper.reflect.ConstructorMapper;
import org.jdbi.core.statement.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs multiple tests against a flyway migrated H2 database. Brings up and tears down database for
 * each test. Ensures that each test gets a new, clean database - addresses #2208
 */
@DisabledInNativeImage // XXX https://github.com/flyway/flyway/issues/2927#issuecomment-2065790934
public class JdbiFlywayH2Test {

    private static final JdbiExtensionInitializer FLYWAY_INITIALIZER = JdbiFlywayMigration.flywayMigration()
            .withPath("test/h2")
            .cleanAfter(true);

    @RegisterExtension
    private final JdbiExtension h2Extension = new JdbiH2Extension("MODE=MySQL;DATABASE_TO_LOWER=TRUE")
            .withUser("user")
            .withInitializer(FLYWAY_INITIALIZER);

    private Jdbi jdbi;

    @BeforeEach
    void setupEach() {
        this.jdbi = h2Extension.getJdbi();
    }

    @Test
    void selectUsersTest() {
        List<User> users = jdbi.withHandle(handle -> {
            try (Query query = handle.createQuery("SELECT * FROM users ORDER BY id")) {
                return query.map(ConstructorMapper.of(User.class)).list();
            }
        });
        assertThat(users).hasSize(2);
    }

    @Test
    void selectAliceTest() {
        User user = jdbi.withHandle(handle -> {
            try (Query query = handle.createQuery("SELECT * FROM users WHERE id = :id")) {
                return query.bind("id", 1)
                        .map(ConstructorMapper.of(User.class))
                        .one();
            }
        });
        assertThat(user).extracting("name", "id").containsExactly("Alice", 1);
    }

    @Test
    void selectBobTest() {
        User user = jdbi.withHandle(handle -> {
            try (Query query = handle.createQuery("SELECT * FROM users WHERE id = :id")) {
                return query.bind("id", 2)
                        .map(ConstructorMapper.of(User.class))
                        .one();
            }
        });
        assertThat(user).extracting("name", "id").containsExactly("Bob", 2);
    }

    public static class User {

        private final int id;
        private final String name;

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            User user = (User) o;
            return id == user.id && Objects.equals(name, user.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }
}

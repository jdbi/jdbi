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
package jdbi.doc;

import java.util.Objects;
import java.util.Optional;

import org.jdbi.core.Jdbi;
import org.jdbi.core.mapper.reflect.ConstructorMapper;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.customizer.BindMethods;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultMethodTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
            .withInitializer(TestingInitializers.usersWithData())
            .withPlugin(new SqlObjectPlugin());

    // tag::dao[]
    interface UserDao {

        @SqlQuery("SELECT * FROM users WHERE id = :id")
        Optional<User> findUser(int id);

        @SqlUpdate("UPDATE users SET name = :name WHERE id = :user.id")
        void updateUser(@BindMethods("user") User user, String name);

        default void changeName(int id, String name) {
            findUser(id).ifPresent(user -> updateUser(user, name));  // <2>
        }
    }
    // end::dao[]

    private Jdbi jdbi;

    @BeforeEach
    void setUp() {
        this.jdbi = h2Extension.getJdbi();
        jdbi.registerRowMapper(User.class, ConstructorMapper.of(User.class));
    }

    // tag::default-method[]
    @Test
    void testDefaultMethod() {
        UserDao dao = jdbi.onDemand(UserDao.class);

        assertThat(dao.findUser(1))
                .isPresent()
                .contains(new User(1, "Alice"));

        dao.changeName(1, "Alex"); // <1>

        assertThat(dao.findUser(1))
                .isPresent()
                .contains(new User(1, "Alex"));
    }
    // end::default-method[]

    public static class User {

        private final int id;

        private final String name;

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int id() {
            return id;
        }

        public String name() {
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

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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Test;

public class IntroductionTest {

    @Test
    public void core() {
        // tag::core[]
        Jdbi jdbi = Jdbi.create("jdbc:h2:mem:test"); // (H2 in-memory database)

        List<User> users = jdbi.withHandle(handle -> {
            handle.execute("CREATE TABLE user (id INTEGER PRIMARY KEY, name VARCHAR)");

            // Inline positional parameters
            handle.execute("INSERT INTO user(id, name) VALUES (?, ?)", 0, "Alice");

            // Positional parameters
            handle.createUpdate("INSERT INTO user(id, name) VALUES (?, ?)")
                    .bind(0, 1) // 0-based parameter indexes
                    .bind(1, "Bob")
                    .execute();

            // Named parameters
            handle.createUpdate("INSERT INTO user(id, name) VALUES (:id, :name)")
                    .bind("id", 2)
                    .bind("name", "Clarice")
                    .execute();

            // Named parameters from bean properties
            handle.createUpdate("INSERT INTO user(id, name) VALUES (:id, :name)")
                    .bindBean(new User(3, "David"))
                    .execute();

            // Easy mapping to any type
            return handle.createQuery("SELECT * FROM user ORDER BY name")
                    .mapToBean(User.class)
                    .list();
        });

        assertThat(users).containsExactly(
                new User(0, "Alice"),
                new User(1, "Bob"),
                new User(2, "Clarice"),
                new User(3, "David"));
        // end::core[]
    }

    // tag::sqlobject-declaration[]
    // Define your own declarative interface
    public interface UserDao {
        @SqlUpdate("CREATE TABLE user (id INTEGER PRIMARY KEY, name VARCHAR)")
        void createTable();

        @SqlUpdate("INSERT INTO user(id, name) VALUES (?, ?)")
        void insertPositional(int id, String name);

        @SqlUpdate("INSERT INTO user(id, name) VALUES (:id, :name)")
        void insertNamed(@Bind("id") int id, @Bind("name") String name);

        @SqlUpdate("INSERT INTO user(id, name) VALUES (:id, :name)")
        void insertBean(@BindBean User user);

        @SqlQuery("SELECT * FROM user ORDER BY name")
        @RegisterBeanMapper(User.class)
        List<User> listUsers();
    }
    // end::sqlobject-declaration[]

    @Test
    public void sqlObject() {
        // tag::sqlobject-usage[]
        Jdbi jdbi = Jdbi.create("jdbc:h2:mem:test");
        jdbi.installPlugin(new SqlObjectPlugin());

        // Jdbi implements your interface based on annotations
        List<User> userNames = jdbi.withExtension(UserDao.class, dao -> {
            dao.createTable();

            dao.insertPositional(0, "Alice");
            dao.insertPositional(1, "Bob");
            dao.insertNamed(2, "Clarice");
            dao.insertBean(new User(3, "David"));

            return dao.listUsers();
        });

        assertThat(userNames).containsExactly(
                new User(0, "Alice"),
                new User(1, "Bob"),
                new User(2, "Clarice"),
                new User(3, "David"));
        // end::sqlobject-usage[]
    }

    public static class User {
        private int id;
        private String name;

        public User() {}

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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
            return id == user.id &&
                    Objects.equals(name, user.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }
}

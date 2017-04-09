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
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
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

            // Easy mapping to your types
            return handle.createQuery("SELECT * FROM user ORDER BY id")
                    .mapToBean(User.class)
                    .list();
        });
        // end::core[]
        assertThat(users).extracting(User::getId, User::getName).containsExactly(
                tuple(0, "Alice"),
                tuple(1, "Bob"),
                tuple(2, "Clarice"),
                tuple(3, "David"));
    }

    // tag::sqlobject-declaration[]
    // Define your own declarative interface
    public interface UserDao {
        @SqlUpdate("CREATE TABLE user (id INTEGER PRIMARY KEY, name VARCHAR)")
        void createTable();

        @SqlUpdate("INSERT INTO user(id, name) VALUES (:id, :name)")
        void insertUser(@Bind("id") int id, @Bind("name") String name);

        @SqlBatch("INSERT INTO user(id, name) VALUES (:id, :name)")
        void insertUsers(@BindBean User... users);

        @SqlQuery("SELECT * FROM user ORDER BY id")
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
        List<User> users = jdbi.withExtension(UserDao.class, dao -> {
            dao.createTable();

            dao.insertUser(0, "Alice");
            dao.insertUsers(new User(1, "Bob"), new User(2, "Clarice"), new User(3, "David"));

            return dao.listUsers();
        });
        // end::sqlobject-usage[]
        assertThat(users).extracting(User::getId, User::getName).containsExactly(
                tuple(0, "Alice"),
                tuple(1, "Bob"),
                tuple(2, "Clarice"),
                tuple(3, "David"));
    }


    public static class User {
        private int id;
        private String name;

        public User() {
        }

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
    }
}

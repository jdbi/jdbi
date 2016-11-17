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

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.mapper.ConstructorMapper;
import org.junit.Test;

public class HelloWorldTest {

    @Test
    public void helloJdbi() {
        // tag::frontPage[]
        // H2 in-memory database
        Jdbi dbi = Jdbi.create("jdbc:h2:mem:test");
        // Easy scope-based transactions
        List<User> users = dbi.inTransaction((handle, status) -> {
            handle.execute("CREATE TABLE user (id INTEGER PRIMARY KEY, name VARCHAR)");
            handle.createUpdate("INSERT INTO user(id, name) VALUES (:id, :name)")
                .bind("id", 0)   // Bind arguments by name
                .bind(1, "You!") // Or by 0-indexed position
                .execute();

            handle.createUpdate("INSERT INTO user(id, name) VALUES (:id, :name)")
                .bindBean(new User(1, "Me")) // You can also bind custom types
                .execute();

            // Easy mapping to your types
            handle.registerRowMapper(ConstructorMapper.of(User.class));
            return handle.createQuery("SELECT * FROM user ORDER BY id")
                .mapTo(User.class)
                .list();
        });
        // end::frontPage[]
        assertThat(users).extracting(User::getId).containsExactly(0, 1);
        assertThat(users).extracting(User::getName).containsExactly("You!", "Me");
    }

    public static class User {
        private final int id;
        private final String name;
        public User(int id, String name) { this.id = id; this.name = name; }
        public int getId() { return id; }
        public String getName() { return name; }
    }
}

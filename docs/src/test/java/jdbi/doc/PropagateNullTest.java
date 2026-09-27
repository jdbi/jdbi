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

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.PropagateNull;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class PropagateNullTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2();

    private Handle handle;

    // tag::types[]
    @PropagateNull("id")
    public static class User {
        final int id;
        final String name;

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class Transfer {
        final int id;
        final User sender;
        final User receiver;

        public Transfer(int id, @Nested("sender") User sender, @Nested("receiver") User receiver) {
            this.id = id;
            this.sender = sender;
            this.receiver = receiver;
        }
    }
    // end::types[]

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();

        handle.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name VARCHAR)");
        handle.execute("CREATE TABLE transfers (id INTEGER PRIMARY KEY, sender_id INTEGER, receiver_id INTEGER)");
        handle.execute("INSERT INTO users (id, name) VALUES (1, 'Alice'), (2, 'Bob')");
        handle.execute("INSERT INTO transfers (id, sender_id, receiver_id) VALUES (10, 1, 2), (11, 2, NULL)");
    }

    @Test
    public void nestedWithPrefix() {
        // tag::nested[]
        List<Transfer> transfers = handle.createQuery(
                "SELECT t.id, s.id AS sender_id, s.name AS sender_name, r.id AS receiver_id, r.name AS receiver_name "
                    + "FROM transfers t "
                    + "JOIN users s ON s.id = t.sender_id "
                    + "LEFT JOIN users r ON r.id = t.receiver_id "
                    + "ORDER BY t.id")
            .map(ConstructorMapper.of(Transfer.class))
            .list();
        // end::nested[]

        assertThat(transfers).hasSize(2);
        assertThat(transfers.get(0).sender.name).isEqualTo("Alice");
        assertThat(transfers.get(0).receiver.name).isEqualTo("Bob");
        assertThat(transfers.get(1).sender.name).isEqualTo("Bob");
        assertThat(transfers.get(1).receiver).isNull();
    }

    @Test
    public void mapperPrefix() {
        // tag::prefix[]
        List<User> receivers = handle.createQuery(
                "SELECT r.id AS receiver_id, r.name AS receiver_name "
                    + "FROM transfers t "
                    + "LEFT JOIN users r ON r.id = t.receiver_id "
                    + "ORDER BY t.id")
            .map(ConstructorMapper.of(User.class, "receiver"))
            .list();
        // end::prefix[]

        assertThat(receivers).hasSize(2);
        assertThat(receivers.get(0).name).isEqualTo("Bob");
        assertThat(receivers.get(1)).isNull();
    }
}

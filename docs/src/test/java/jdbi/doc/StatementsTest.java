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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class StatementsTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2();

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
        handle.execute("CREATE TABLE \"user\" (id INTEGER PRIMARY KEY, \"name\" VARCHAR)");
        handle.execute("INSERT INTO \"user\" VALUES (1, 'Alice')");
        handle.execute("INSERT INTO \"user\" VALUES (2, 'Bob')");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testQuery() {
        // tag::query[]
        List<Map<String, Object>> users =
            handle.createQuery("SELECT id, \"name\" FROM \"user\" ORDER BY id ASC")
                .mapToMap()
                .list();

        assertThat(users).containsExactly(
                map("id", 1, "name", "Alice"),
                map("id", 2, "name", "Bob"));
        // end::query[]
    }

    @Test
    public void testUpdate() {
        // tag::update[]
        int count = handle.createUpdate("INSERT INTO \"user\" (id, \"name\") VALUES(:id, :name)")
            .bind("id", 3)
            .bind("name", "Charlie")
            .execute();
        assertThat(count).isOne();
        // end::update[]

        // tag::execute[]
        count = handle.execute("INSERT INTO \"user\" (id, \"name\") VALUES(?, ?)", 4, "Alice");
        assertThat(count).isOne();
        // end::execute[]
    }

    @Test
    public void testScript() {
        // tag::script[]
        int[] results = handle.createScript(
                "INSERT INTO \"user\" VALUES(3, 'Charlie');"
                + "UPDATE \"user\" SET \"name\"='Bobby Tables' WHERE id=2;")
            .execute();

        assertThat(results).containsExactly(1, 1);
        // end::script[]
    }

    @Test
    public void testBatch() {
        // tag::batch[]
        PreparedBatch batch = handle.prepareBatch("INSERT INTO \"user\" (id, \"name\") VALUES(:id, :name)");
        for (int i = 100; i < 5000; i++) {
            batch.bind("id", i).bind("name", "User:" + i).add();
        }
        int[] counts = batch.execute();
        // end::batch[]

        int[] expected = new int[4900];
        Arrays.fill(expected, 1);
        assertThat(counts).isEqualTo(expected);
    }

    static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2) {
        HashMap<K, V> h = new HashMap<>();
        h.put(k1, v1);
        h.put(k2, v2);
        return h;
    }
}

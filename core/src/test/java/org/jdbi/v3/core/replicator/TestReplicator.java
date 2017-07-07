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
package org.jdbi.v3.core.replicator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.stream.Collectors;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestReplicator {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new ReplicatorPlugin());
    private Handle h;

    @Before
    public void setup() throws Exception {
        h = db.getSharedHandle();
        h.execute("CREATE TABLE replicator (name varchar, value int, something uuid)");
        h.execute("INSERT INTO replicator VALUES (?,?,?)", "alice", 42, new UUID(42, 42));
    }

    @Test
    public void testAutoTypeReplicator() throws Exception {
        final SimpleResult r = h.createQuery("SELECT * FROM replicator").mapTo(SimpleResult.class).findOnly();
        assertThat(r.getName()).isEqualTo("alice");
        assertThat(r.getValue()).isEqualTo(42);
        assertThat(r.getUuid()).isEqualTo(new UUID(42, 42));
    }

    @Test(expected = UnableToProduceResultException.class)
    public void testMissingColumn() {
        h.createQuery("SELECT name, value FROM replicator").mapTo(SimpleResult.class).findOnly();
    }

    @Test
    public void testToString() {
        final SimpleResult r = h.createQuery("SELECT * FROM replicator").mapTo(SimpleResult.class).findOnly();
        assertThat(r.toString()).contains("getName()=alice");
    }

    @Test
    public void testHashCodeEquals() {
        h.execute("INSERT INTO replicator VALUES (?,?,?)", "alice", 42, new UUID(42, 42));
        h.execute("INSERT INTO replicator VALUES (?,?,?)", "bob", 42, new UUID(42, 42));
        assertThat(h.createQuery("SELECT * FROM replicator").mapTo(SimpleResult.class).collect(Collectors.toSet()))
            .hasSize(2);
    }

    @Replicated
    public interface SimpleResult {
        String getName();
        int getValue();
        @ColumnName("something")
        UUID getUuid();
    }
}

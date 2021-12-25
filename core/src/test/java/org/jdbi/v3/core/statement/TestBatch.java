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
package org.jdbi.v3.core.statement;

import java.sql.Types;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBatch {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    @Test
    public void testBasics() {
        Handle h = h2Extension.openHandle();

        Batch b = h.createBatch();
        b.add("insert into something (id, name) values (0, 'Keith')");
        b.add("insert into something (id, name) values (1, 'Eric')");
        b.add("insert into something (id, name) values (2, 'Brian')");
        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).hasSize(3);
    }

    @Test
    public void testEmptyBatchThrows() {
        try (Handle h = h2Extension.openHandle()) {
            final PreparedBatch b = h.prepareBatch("insert into something (id, name) values (?, ?)");
            assertThatThrownBy(b::add).isInstanceOf(IllegalStateException.class); // No parameters written yet
        }
    }

    @Test
    public void testPreparedBatch() throws Exception {
        int batchCount = 50;
        try (Handle h = h2Extension.openHandle()) {
            PreparedBatch batch = h.prepareBatch("INSERT INTO something (id, name) VALUES(:id, :name)");
            for (int i = 1; i <= batchCount; i++) {
                batch.bind("id", i)
                    .bind("name", "User:" + i)
                    .add();
            }
            int[] counts = batch.execute();

            assertEquals(batchCount, counts.length);
            for (int i = 0; i < batchCount; i++) {
                assertEquals(1, counts[i]);
            }
        }
    }

    @Test
    public void testPreparedBatchWithNull() throws Exception {
        int batchCount = 5;
        try (Handle h = h2Extension.openHandle()) {
            PreparedBatch batch = h.prepareBatch("INSERT INTO something (id, name) VALUES(:id, :name)");
            for (int i = 1; i <= batchCount; i++) {
                batch.bind("id", i)
                    .bindNull("name", Types.OTHER)
                    .add();
            }
            int[] counts = batch.execute();

            assertEquals(batchCount, counts.length);
            for (int i = 0; i < batchCount; i++) {
                assertEquals(1, counts[i]);
            }
        }
    }
}

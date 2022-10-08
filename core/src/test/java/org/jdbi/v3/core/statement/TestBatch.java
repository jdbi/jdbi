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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

            assertThat(batchCount).isEqualTo(counts.length);
            for (int i = 0; i < batchCount; i++) {
                assertThat(counts[i]).isOne();
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

            assertThat(batchCount).isEqualTo(counts.length);
            for (int i = 0; i < batchCount; i++) {
                assertThat(counts[i]).isOne();
            }
        }
    }

    public static class BatchRecord {
        private final UUID id, next;
        private final long createdAt, updatedAt;

        public BatchRecord(UUID id, UUID next, long createdAt, long updatedAt) {
            this.id = id;
            this.next = next;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public UUID getId() {
            return id;
        }

        public UUID getNext() {
            return next;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getUpdatedAt() {
            return updatedAt;
        }

        @Override
        public int hashCode() {
            return Objects.hash(createdAt, id, next, updatedAt);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            BatchRecord other = (BatchRecord) obj;
            return createdAt == other.createdAt && Objects.equals(id, other.id)
                    && Objects.equals(next, other.next)
                    && updatedAt == other.updatedAt;
        }

        @Override
        public String toString() {
            return "BatchRecord [id=" + id + ", next=" + next + ", createdAt="
                    + createdAt + ", updatedAt=" + updatedAt + "]";
        }
    }

    @Test
    public void testBatchBinding1987() {
        Handle h = h2Extension.getSharedHandle();
        h.execute("CREATE TABLE recs ("
                + "id uuid PRIMARY KEY,"
                + "next uuid,"
                + "created_at bigint NOT NULL,"
                + "updated_at bigint NOT NULL"
                + ");");

        UUID u1 = new UUID(0, 0);
        UUID u2 = new UUID(1, 1);
        List<BatchRecord> recs = Arrays.asList(
                new BatchRecord(u1, u2, 0, 2),
                new BatchRecord(u2, null, 0, 2));

        h.useTransaction(txn -> {
            PreparedBatch batch = txn.prepareBatch(
                    "INSERT INTO recs (id, next, created_at, updated_at) VALUES (:id, :next, :created_at, :updated_at)");
            for (BatchRecord rec : recs) {
                batch.bind("id", rec.getId())
                        .bindBySqlType("next", rec.getNext(), Types.OTHER)
                        .bind("created_at", rec.getCreatedAt())
                        .bind("updated_at", rec.getUpdatedAt())
                        .add();
            }
            batch.execute();
        });

        assertThat(h.createQuery("SELECT * FROM recs")
                .registerRowMapper(ConstructorMapper.factory(BatchRecord.class))
                .mapTo(BatchRecord.class)
                .list())
            .containsExactlyInAnyOrderElementsOf(recs);
    }
}

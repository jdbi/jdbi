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
package org.jdbi.v3.sqlobject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.statement.BatchChunkSize;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.Arrays.stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestBatchingSingleValue {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin()).withPlugin(new H2DatabasePlugin());

    private Handle handle;
    private SingleValueBatching b;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
        handle.execute("create table batching (id integer, vals integer array)");
        b = handle.attach(SingleValueBatching.class);
    }

    @Test
    public void testSingleValueArray() {
        final int[] ids = IntStream.range(0, 10).toArray();
        final int[] values = IntStream.range(50, 60).toArray();

        b.insertValues(ids, values);

        assertThat(b.select()).containsExactly(
                stream(ids)
                    .mapToObj(id -> new BatchingRow(id, values))
                    .toArray(BatchingRow[]::new));
    }

    @BatchChunkSize(4)
    @RegisterConstructorMapper(BatchingRow.class)
    public interface SingleValueBatching {
        @SqlBatch("insert into batching (id, vals) values (:id, :vals)")
        int[] insertValues(int[] id, @SingleValue int[] vals);

        @SqlQuery("select id, vals from batching order by id asc")
        List<BatchingRow> select();
    }

    public static class BatchingRow {
        final int id;
        final int[] vals;

        public BatchingRow(int id, int[] vals) {
            this.id = id;
            this.vals = vals;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BatchingRow other) {
                return id == other.id && Arrays.equals(vals, other.vals);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id ^ Arrays.hashCode(vals);
        }

        @Override
        public String toString() {
            return String.format("%s %s", id, Arrays.toString(vals));
        }
    }
}

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

import java.util.List;
import java.util.stream.IntStream;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.statement.BatchChunkSize;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.util.Arrays.stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestBatchingSingleValue {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin()).withPlugin(new H2DatabasePlugin());
    private Handle handle;
    private SingleValueBatching b;

    @Before
    public void setUp() {
        handle = dbRule.getSharedHandle();
        handle.execute("create table batching (id integer, values array)");
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
        @SqlBatch("insert into batching (id, values) values (:id, :values)")
        int[] insertValues(int[] id, @SingleValue int[] values);

        @SqlQuery("select id, values from batching order by id asc")
        List<BatchingRow> select();
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode
    @ToString
    public static class BatchingRow {
        final int id;
        final int[] values;
    }
}

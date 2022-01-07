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
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBatchProblems {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    @Test
    public void testNonUniformInserts() throws Exception {
        int batchCount = 50;

        try (Handle h = h2Extension.openHandle()) {
            for (int i = 101; i <= 100 + batchCount; i++) {
                Map<String, Object> data;
                if (i % 2 == 0) {
                    data = ImmutableMap.of("id", i, "name", "User:" + i);
                } else {
                    data = ImmutableMap.of("id", i, "name", i);
                }

                int result = h.createUpdate("INSERT INTO something (id, name) VALUES(:id, :name)")
                    .bindMap(data)
                    .execute();

                assertEquals(1, result);
            }
            int result = h.createQuery("SELECT COUNT(1) FROM something").mapTo(Integer.class).first();
            assertEquals(batchCount, result);
        }
    }

    @Test
    public void testBatchStringInserts() throws Exception {
        int batchCount = 50;

        try (Handle h = h2Extension.openHandle()) {
            PreparedBatch batch = h.prepareBatch("INSERT INTO something (id, name) VALUES(:id, :name)");
            for (int i = 1; i <= batchCount; i++) {
                batch.bind("id", i).bind("name", "User:" + i).add();
            }

            int[] counts = batch.execute();

            assertEquals(batchCount, counts.length);
            for (int i = 0; i < batchCount; i++) {
                assertEquals(1, counts[i]);
            }
        }
    }


    @Test
    public void testBatchIntInserts() throws Exception {
        int batchCount = 50;

        try (Handle h = h2Extension.openHandle()) {
            PreparedBatch batch = h.prepareBatch("INSERT INTO something (id, name) VALUES(:id, :name)");
            for (int i = 1; i <= batchCount; i++) {
                batch.bind("id", i).bindBySqlType("name", i, Types.VARCHAR).add();
            }

            int[] counts = batch.execute();

            assertEquals(batchCount, counts.length);
            for (int i = 0; i < batchCount; i++) {
                assertEquals(1, counts[i]);
            }
        }
    }


    @Test
    public void testNonUniformBatchInserts() throws Exception {
        int batchCount = 50;

        try (Handle h = h2Extension.openHandle()) {
            PreparedBatch batch = h.prepareBatch("INSERT INTO something (id, name) VALUES(:id, :name)");
            for (int i = 1; i <= batchCount; i++) {
                Map<String, Object> data;
                if (i % 2 == 0) {
                    data = ImmutableMap.of("id", i, "name", "User:" + i);
                } else {
                    data = ImmutableMap.of("id", i, "name", i);
                }

                batch.bindMap(data).add();
            }

            int[] counts = batch.execute();

            assertEquals(batchCount, counts.length);
            for (int i = 0; i < batchCount; i++) {
                assertEquals(1, counts[i]);
            }
        }
    }

    @Test
    public void testNonUniformBatchInsertsWithArguments() throws Exception {
        int batchCount = 50;

        try (Handle h = h2Extension.openHandle()) {
            PreparedBatch batch = h.prepareBatch("INSERT INTO something (id, name) VALUES(:id, :name)");
            for (int i = 1; i <= batchCount; i++) {
                if (i % 2 == 0) {
                    batch.bind("id", i).bind("name", "User:" + i).add();
                } else {
                    batch.bind("id", i).bindBySqlType("name", i, Types.VARCHAR).add();
                }
            }

            int[] counts = batch.execute();

            assertEquals(batchCount, counts.length);
            for (int i = 0; i < batchCount; i++) {
                assertEquals(1, counts[i]);
            }
        }
    }
}

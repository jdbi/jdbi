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

import java.util.List;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.PgDatabaseExtension;
import org.jdbi.v3.core.result.ResultProducers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.atIndex;

public class TestPreparedBatchPG {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public PgDatabaseExtension pgExtension = PgDatabaseExtension.instance(pg).withInitializer(h -> h.execute("create table something (id integer primary key, name varchar(50), integerValue integer, intValue integer)"));

    private Handle handle;

    @BeforeEach
    public void setUp() {
        this.handle = pgExtension.getSharedHandle();
    }

    @Test
    public void emptyBatch() {
        try (PreparedBatch batch = handle.prepareBatch("insert into something (id, name) values (:id, :name)")) {
            assertThat(batch.execute()).isEmpty();
        }
    }

    // This would be a test in `TestPreparedBatch` but H2 has a bug (?) that returns a generated key even when there wasn't one.
    @Test
    public void emptyBatchGeneratedKeys() {
        try (PreparedBatch batch = handle.prepareBatch("insert into something (id, name) values (:id, :name)")) {
            assertThat(batch.executePreparedBatch("id").mapTo(int.class).list()).isEmpty();
        }
    }

    @Test
    public void testBatchReturningGeneratedKeys() {
        try (PreparedBatch b = handle.prepareBatch("insert into something (id, name) values (:id, :name) RETURNING name")) {
            b.bind("id", 1).bind("name", "Eric").add();
            b.bind("id", 2).bind("name", "Brian").add();
            b.bind("id", 3).bind("name", "Keith").add();
            List<String> results = b.execute(ResultProducers.returningGeneratedKeys("name")).mapTo(String.class).list();

            assertThat(results).hasSize(3);
            assertThat(results).contains("Eric", atIndex(0));
            assertThat(results).contains("Brian", atIndex(1));
            assertThat(results).contains("Keith", atIndex(2));
        }
    }
}

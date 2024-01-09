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
package org.jdbi.v3.e2e;

import java.util.Arrays;
import java.util.List;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.result.BatchResultBearing;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestIssue2564 {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withInitializer(
        (ds, h) -> h.execute("create table something (id serial, name varchar(50), create_time timestamp default now())")
    );

    Handle handle;

    @BeforeEach
    void setUp() {
        this.handle = pgExtension.getSharedHandle();
    }

    @Test
    void testBatchInsertReturningIds() {
        List<String> names = Arrays.asList(
            "Brian",
            "Steven",
            "Matthew",
            "Artem",
            "Marnick",
            "Henning");


        PreparedBatch batch = handle.prepareBatch("insert into something (name) values (?) returning id");
        names.forEach(batch::add);

        BatchResultBearing batchResult = batch.executePreparedBatch("id");
        List<Integer> ids = batchResult.mapTo(Integer.class).list();

        assertThat(ids).hasSize(6)
            .containsExactly(1, 2, 3, 4, 5, 6);
    }
}

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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.PgDatabaseRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPreparedBatchPG {
    @Rule
    public PgDatabaseRule dbRule = new PgDatabaseRule();

    private Handle h;

    @Before
    public void openHandle() {
        h = dbRule.openHandle();
        h.execute("create table something (id int primary key, name varchar not null)");
    }

    @After
    public void closeHandle() {
        h.close();
    }

    @Test
    public void emptyBatch() {
        assertThat(h.prepareBatch("insert into something (id, name) values (:id, :name)").execute()).isEmpty();
    }

    // This would be a test in `TestPreparedBatch` but H2 has a bug (?) that returns a generated key even when there wasn't one.
    @Test
    public void emptyBatchGeneratedKeys() {
        assertThat(
                h.prepareBatch("insert into something (id, name) values (:id, :name)")
                 .executeAndReturnGeneratedKeys("id")
                 .mapTo(int.class)
                 .list())
            .isEmpty();
    }
}

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
package org.jdbi.v3.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestUuid {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    public Jdbi db;
    public Handle h;

    @Before
    public void setupDbi() {
        db = dbRule.getJdbi();

        h = db.open();
        h.execute("CREATE TABLE foo (bar UUID)");
    }

    @After
    public void tearDown() {
        h.close();
    }

    @Test
    public void testUuid() {
        UUID u = UUID.randomUUID();
        h.createUpdate("INSERT INTO foo VALUES (:uuid)")
            .bind("uuid", u)
            .execute();

        assertThat(h.createQuery("SELECT * FROM foo").mapTo(UUID.class).findOnly()).isEqualTo(u);
    }
}

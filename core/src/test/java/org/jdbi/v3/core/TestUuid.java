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

import java.util.UUID;

import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUuid {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    public Jdbi db;
    public Handle h;

    @BeforeEach
    public void setupDbi() {
        db = h2Extension.getJdbi();

        h = db.open();
        h.execute("CREATE TABLE foo (bar UUID)");
    }

    @AfterEach
    public void tearDown() {
        h.close();
    }

    @Test
    public void testUuid() {
        UUID u = UUID.randomUUID();
        h.createUpdate("INSERT INTO foo VALUES (:uuid)")
            .bind("uuid", u)
            .execute();

        assertThat(h.createQuery("SELECT * FROM foo").mapTo(UUID.class).one()).isEqualTo(u);
    }
}

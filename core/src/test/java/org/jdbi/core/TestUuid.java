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
package org.jdbi.core;

import java.util.UUID;

import org.jdbi.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUuid {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @BeforeEach
    public void setupDbi() {
        h2Extension.getSharedHandle().execute("CREATE TABLE foo (bar UUID)");
    }

    @Test
    public void testUuid() {
        Handle h = h2Extension.getSharedHandle();

        UUID u = UUID.randomUUID();
        h.createUpdate("INSERT INTO foo VALUES (:uuid)")
            .bind("uuid", u)
            .execute();

        assertThat(h.createQuery("SELECT * FROM foo").mapTo(UUID.class).one()).isEqualTo(u);
    }
}

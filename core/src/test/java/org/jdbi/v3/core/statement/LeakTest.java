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
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Many tests that try to provoke leaks and see whether the various checkers find them and various cleanup strategies catch them.
 */

public class LeakTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance()
        .withInitializer(H2DatabaseExtension.USERS_INITIALIZER);

    private Handle handle;

    @BeforeEach
    public void setUp() {
        this.handle = h2Extension.getSharedHandle();
    }

    @Test
    public void testScript() {
        int[] results = handle.createScript("INSERT INTO users (id, name) VALUES(3, 'Charlie');"
                + "UPDATE users SET name='Bobby Tables' WHERE id=2;")
            .execute();

        assertThat(results).containsExactly(1, 1);
    }

    @Test
    public void testScriptAsSeparateStatements() {
        handle.createScript("INSERT INTO users (id, name) VALUES(3, 'Charlie');"
                + "UPDATE users SET name='Bobby Tables' WHERE id=2;")
            .executeAsSeparateStatements();
    }

    @Test
    public void testScriptTwr() {
        try (Script script = handle.createScript("INSERT INTO users (id, name) VALUES(3, 'Charlie');"
            + "UPDATE users SET name='Bobby Tables' WHERE id=2;")) {
            int[] results = script.execute();
            assertThat(results).containsExactly(1, 1);
        }
    }
}

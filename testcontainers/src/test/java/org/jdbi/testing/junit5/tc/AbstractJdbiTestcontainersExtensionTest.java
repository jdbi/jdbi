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
package org.jdbi.testing.junit5.tc;

import java.util.List;

import org.jdbi.core.statement.Query;
import org.jdbi.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.JdbcDatabaseContainer;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractJdbiTestcontainersExtensionTest {

    abstract JdbcDatabaseContainer<?> getDbContainer();

    protected String getTableCreateStatement() {
        return "CREATE TABLE users (id INTEGER, name VARCHAR(255))";
    }

    @RegisterExtension
    JdbiExtension extension = JdbiTestcontainersExtension.instance(getDbContainer())
        .withInitializer((ds, handle) -> {
            handle.execute(getTableCreateStatement());
            handle.execute("INSERT INTO users VALUES (?, ?)", 1, "Alice");
            handle.execute("INSERT INTO users VALUES (?, ?)", 2, "Bob");
        });

    @Test
    void testOne() {
        runTest();
    }

    @Test
    void testTwo() {
        runTest();
    }

    @Test
    void testThree() {
        runTest();
    }

    private void runTest() {
        List<String> userNames = extension.getJdbi().withHandle(h -> {
            try (Query query = h.createQuery("SELECT name FROM users ORDER BY id")) {
                return query.mapTo(String.class).list();
            }
        });

        assertThat(userNames).hasSize(2);
        assertThat(userNames).containsExactly("Alice", "Bob");
    }
}

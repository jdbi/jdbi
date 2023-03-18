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
import java.util.Optional;
import java.util.stream.Stream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.result.ResultIterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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

    @Test
    void testManagedHandleExplodingStatementNeedsHandleCleanup() {
        assertThatExceptionOfType(UnableToExecuteStatementException.class).isThrownBy(() ->
                h2Extension.getJdbi().useHandle(managedHandle -> {
                    List<String> userNames = managedHandle.createQuery("SELECT name from users")
                            .attachToHandleForCleanup()
                            .setFetchSize(-1)
                            .mapTo(String.class)
                            .list();
                }));
    }

    @Test
    void testManagedTransactionExplodingStatementNeedsHandleCleanup() {
        assertThatExceptionOfType(UnableToExecuteStatementException.class).isThrownBy(() ->
                h2Extension.getJdbi().useTransaction(managedHandle -> {
                    List<String> userNames = managedHandle.createQuery("SELECT name from users")
                            .attachToHandleForCleanup()
                            .setFetchSize(-1)
                            .mapTo(String.class)
                            .list();
                }));
    }

    @Test
    void testUnmanagedHandleExplodingStatementNeedsHandleCleanup() {
        assertThatExceptionOfType(UnableToExecuteStatementException.class).isThrownBy(() -> {
            try (Handle handle = h2Extension.openHandle()) {
                List<String> userNames = handle.createQuery("SELECT name from users")
                        .attachToHandleForCleanup() // otherwise leaks the query statement
                        .setFetchSize(-1)
                        .mapTo(String.class)
                        .list();
            }
        });
    }

    @Test
    void testUnmanagedHandleExplodingStatemenCleanupBySetting() {
        h2Extension.getJdbi().getConfig(SqlStatements.class).setAttachAllStatementsForCleanup(true);

        assertThatExceptionOfType(UnableToExecuteStatementException.class).isThrownBy(() -> {
            try (Handle handle = h2Extension.openHandle()) {
                List<String> userNames = handle.createQuery("SELECT name from users")
                        .setFetchSize(-1)
                        .mapTo(String.class)
                        .list();
            }
        });
    }

    @Test
    void testStatementStreamCleanupBySetting() {
        h2Extension.getJdbi().getConfig(SqlStatements.class).setAttachAllStatementsForCleanup(true);

        try (Handle handle = h2Extension.openHandle()) {
            Optional<String> userName = handle.createQuery("SELECT name from users")
                    .mapTo(String.class)
                    .stream().findFirst();
            assertThat(userName).isPresent();
        }
    }

    @Test
    void testStatementStreamCleanupByHandleSetting() {

        try (Handle handle = h2Extension.openHandle()) {
            handle.getConfig(SqlStatements.class).setAttachAllStatementsForCleanup(true);
            Optional<String> userName = handle.createQuery("SELECT name from users")
                    .mapTo(String.class)
                    .stream().findFirst();
            assertThat(userName).isPresent();
        }
    }

    @Test
    void testStatementStreamCallbackCleanup() {
        final Jdbi jdbi = h2Extension.getJdbi();

        Optional<String> userName = jdbi.withHandle(h ->
                h.createQuery("SELECT name from users")
                        .mapTo(String.class)
                        .stream().findFirst());

        assertThat(userName).isPresent();
    }

    @Test
    void testStatementManagedStream() {
        try (Handle handle = h2Extension.openHandle()) {
            try (Query query = handle.createQuery("SELECT name from users")) {
                Optional<String> userName = query
                        .mapTo(String.class)
                        .stream().findFirst();
                assertThat(userName).isPresent();
            }
        }
    }

    @Test
    void testDirectlyManagedStream() {
        try (Handle handle = h2Extension.openHandle()) {
            try (Stream<String> stream = handle.createQuery("SELECT name from users")
                    .mapTo(String.class)
                    .stream()) {
                Optional<String> userName = stream.findFirst();
                assertThat(userName).isPresent();
            }
        }
    }

    @Test
    void testDirectlyManagedIterator() {
        try (Handle handle = h2Extension.openHandle()) {
            try (ResultIterator<String> it = handle.createQuery("SELECT name from users")
                    .mapTo(String.class)
                    .iterator()) {
                assertThat(it).hasNext();
                String userName = it.next();
            }
        }
    }
}

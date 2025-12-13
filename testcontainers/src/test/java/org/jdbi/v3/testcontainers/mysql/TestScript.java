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
package org.jdbi.v3.testcontainers.mysql;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.locator.ClasspathSqlLocator;
import org.jdbi.v3.core.statement.Script;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@Testcontainers
public class TestScript {
    static final String MYSQL_VERSION = System.getProperty("jdbi.test.mysql-version", "mysql");
    @Container
    static JdbcDatabaseContainer<?> dbContainer = new MySQLContainer<>(MYSQL_VERSION).withUrlParam("rewriteBatchedStatements", "true");

    @RegisterExtension
    JdbiExtension extension = JdbiTestcontainersExtension.instance(dbContainer);

    @BeforeEach
    public void setUp() {
        extension.getSharedHandle().getConfig(SqlStatements.class).setScriptStatementsNeedSemicolon(false);
    }

    @Test
    void testIssue2554Script() {
        Handle h = extension.getSharedHandle();
        String sql = ClasspathSqlLocator.removingComments()
            .getResource("scripts/test-issue-2554.sql");
        try (Script script = new Script(h, sql)) {
            List<String> statements = script.getStatements();
            assertThat(statements).hasSize(5);

            for (String statement : statements) {
                assertThat(statement).doesNotEndWith(";");
            }
        }
    }

    @Test
    void testIssue2554() {
        Handle h = extension.getSharedHandle();
        String sql = ClasspathSqlLocator.removingComments()
            .getResource("scripts/test-issue-2554.sql");
        try (Script script = new Script(h, sql)) {
            int[] outcome = script.execute();
            assertThat(outcome)
                .hasSize(5)
                .containsExactly(0, -1, -1, -1, -1);
        }
    }
}

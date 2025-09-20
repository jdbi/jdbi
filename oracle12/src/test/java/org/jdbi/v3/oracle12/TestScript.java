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
package org.jdbi.v3.oracle12;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.StringJoiner;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.locator.ClasspathSqlLocator;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Script;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@Testcontainers
public class TestScript {

    static final String CONTAINER_VERSION = "gvenzl/oracle-free:" + System.getProperty("oracle.container.version", "slim-faststart");

    @Container
    static OracleContainer oc = new OracleContainer(CONTAINER_VERSION);

    @RegisterExtension
    public JdbiExtension oracleExtension = JdbiTestcontainersExtension.instance(oc);

    @Test
    void testStatementParsing() {
        Handle h = oracleExtension.getSharedHandle();
        String sql = ClasspathSqlLocator.removingComments()
            .getResource("scripts/oracle-issue-2021.sql");
        try (Script script = new Script(h, sql)) {
            List<String> statements = script.getStatements();
            assertThat(statements).hasSize(3);

            for (String statement : statements) {
                assertThat(statement).doesNotEndWithIgnoringCase("end"); // end needs to be trailed by semicolon
            }
        }
    }

    @Test
    void testIssue2021() {
        Handle h = oracleExtension.getSharedHandle();
        String sql = ClasspathSqlLocator.removingComments()
            .getResource("scripts/oracle-issue-2021.sql");
        try (Script script = new Script(h, sql)) {
            int[] outcome = script.execute();
            assertThat(outcome)
                .hasSize(3)
                .containsExactly(0, 0, 0);
        }

        for (int i = 0; i < 10; i++) {
            int rows = h.createUpdate("INSERT INTO EXAMPLE (VALUE) VALUES (:value)")
                .bind("value", "TEST" + i)
                .execute();
            assertThat(rows).isOne();
        }

        try (Query q = h.createQuery("SELECT * FROM EXAMPLE ORDER BY ID")) {
            List<Example> result = q.map(new ExampleMapper()).list();
            assertThat(result).hasSize(10);

            for (int i = 0; i < result.size(); i++) {
                Example example = result.get(i);
                assertThat(example.id()).isEqualTo(i + 1); // ID starts at one
                assertThat(example.username()).isEqualToIgnoringCase("SYSTEM");
                assertThat(example.value()).isEqualTo("TEST" + i);
            }
        }
    }

    public static final class Example {

        private final long id;
        private final String username;
        private final String value;

        public Example(long id, String username, String value) {
            this.id = id;
            this.username = username;
            this.value = value;
        }

        public long id() {
            return id;
        }

        public String username() {
            return username;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Example.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("username='" + username + "'")
                .add("value='" + value + "'")
                .toString();
        }
    }

    public static final class ExampleMapper implements RowMapper<Example> {

        @Override
        public Example map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Example(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("value")
            );
        }
    }
}

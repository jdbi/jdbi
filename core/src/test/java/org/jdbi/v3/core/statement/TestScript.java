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
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.junit5.PgDatabaseExtension;
import org.jdbi.v3.core.locator.ClasspathSqlLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class TestScript {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public PgDatabaseExtension pgExtension = PgDatabaseExtension.instance(pg).withInitializer(h -> h.execute("create table something (id serial, data json)"));

    @Test
    public void testScriptStuff() {
        Handle h = h2Extension.getSharedHandle();
        try (Script s = h.createScript(getClasspathSqlLocator().locate("default-data"))) {
            s.execute();

            assertThat(h.select("select * from something").mapToMap()).hasSize(2);
        }
    }

    @Test
    public void testScriptWithComments() {
        Handle h = h2Extension.getSharedHandle();
        try (Script script = h.createScript(getClasspathSqlLocator().getResource("script/insert-script-with-comments.sql"))) {
            script.execute();

            assertThat(h.select("select * from something").mapToMap()).hasSize(3);
        }
    }

    @Test
    public void testScriptWithStringSemicolon() {
        Handle h = h2Extension.getSharedHandle();
        try (Script script = h.createScript(getClasspathSqlLocator().getResource("script/insert-with-string-semicolons.sql"))) {
            script.execute();

            assertThat(h.select("select * from something").mapToMap()).hasSize(3);
        }
    }

    @Test
    public void testFuzzyScript() {
        Handle h = h2Extension.getSharedHandle();
        try (Script script = h.createScript(getClasspathSqlLocator().getResource("script/fuzzy-script.sql"))) {
            script.executeAsSeparateStatements();

            List<Map<String, Object>> rows = h.select("select id, name from something order by id").mapToMap().list();
            assertThat(rows).isEqualTo(ImmutableList.of(
                ImmutableMap.of("id", 1L, "name", "eric"),
                ImmutableMap.of("id", 2L, "name", "sally;ann"),
                ImmutableMap.of("id", 3L, "name", "bob"),
                ImmutableMap.of("id", 12L, "name", "sally;ann;junior")));
        }
    }

    @Test
    public void testScriptAsSetOfSeparateStatements() {
        assertThatExceptionOfType(StatementException.class)
            .isThrownBy(() -> {
                Handle h = h2Extension.getSharedHandle();
                try (Script script = h.createScript(getClasspathSqlLocator().getResource("script/malformed-sql-script.sql"))) {
                    script.executeAsSeparateStatements();
                }
            })
            .satisfies(e -> assertThat(e.getStatementContext().getRawSql().trim())
                .isEqualTo("insert into something(id, name) values (2, eric)"));
    }

    @Test
    public void testPostgresJsonExtractTextOperator() {
        Handle h = pgExtension.getSharedHandle();
        try (Script script = h.createScript(getClasspathSqlLocator().getResource("script/postgres-json-operator.sql"))) {
            script.execute();

            assertThat(h.select("select * from something").mapToMap()).hasSize(1);
        }
    }

    @Test
    public void testOracleScript() {
        Handle h = h2Extension.getSharedHandle();
        String sql = getClasspathSqlLocator().getResource("script/oracle-issue-2021.sql");

        try (Script script = new Script(h, sql)) {

            List<String> statements = script.getStatements();

            assertThat(statements).hasSize(3);

            for (String statement : statements) {
                assertThat(statement).doesNotEndWithIgnoringCase("end");
            }
        }
    }

    @Test
    public void testMySQLScript() {
        Handle h = h2Extension.getSharedHandle();
        h.getConfig(SqlStatements.class).setScriptStatementsNeedSemicolon(false);
        String sql = getClasspathSqlLocator().getResource("script/oracle-issue-2021.sql");

        try (Script script = new Script(h, sql)) {
            List<String> statements = script.getStatements();

            assertThat(statements).hasSize(3);

            for (String statement : statements) {
                assertThat(statement).doesNotEndWithIgnoringCase("end;");
            }
        }
    }

    private ClasspathSqlLocator getClasspathSqlLocator() {
        return ClasspathSqlLocator.removingComments();
    }
}

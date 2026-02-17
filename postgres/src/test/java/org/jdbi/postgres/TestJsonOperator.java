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
package org.jdbi.postgres;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.Maps;
import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import de.softwareforge.testing.postgres.junit5.RequirePostgresVersion;
import org.jdbi.core.Jdbi;
import org.jdbi.core.spi.JdbiPlugin;
import org.jdbi.core.statement.SqlStatements;
import org.jdbi.core.statement.TemplateEngine;
import org.jdbi.stringtemplate4.StringTemplateSqlLocator;
import org.jdbi.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import static org.assertj.core.api.Assertions.assertThat;

@RequirePostgresVersion(atLeast = "10")
public class TestJsonOperator {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg)
            .withPlugins(new PostgresPlugin(), new StringTemplateLocatorPlugin(TestJsonOperator.class));


    /**
     * Examples from <a href="https://www.postgresql.org/docs/current/static/functions-json.html">Postgres JSON Functions</a>. Escaping rules from <a
     * href="https://jdbc.postgresql.org/documentation/head/statement.html">Postgres Prepared Statements</a>.
     */

    @TestFactory
    @RequirePostgresVersion(atLeast = "10")
    public Stream<DynamicTest> testPostgres10JsonFunctions() {
        return findDynamicTests("10");
    }

    @TestFactory
    @RequirePostgresVersion(atLeast = "12")
    public Stream<DynamicTest> testPostgres12JsonFunctions() {
        return findDynamicTests("12");
    }

    @TestFactory
    @RequirePostgresVersion(atLeast = "13")
    public Stream<DynamicTest> testPostgres13JsonFunctions() {
        return findDynamicTests("13");
    }

    @Test
    public void testJsonQueryWithBoundInput() {
        assertThat(pgExtension.getSharedHandle()
                .createQuery("SELECT '{\"a\":1, \"b\":2}'::jsonb ?? :key")
                .bind("key", "a")
                .mapTo(boolean.class)
                .one())
                .isTrue();
    }

    private Stream<DynamicTest> findDynamicTests(String version) {

        // tests and their results are stored in TestJsonOperator.sql.stg in the resources
        final STGroup parent = StringTemplateSqlLocator.findStringTemplateGroup(TestJsonOperator.class);
        final Map<String, STGroup> children = Maps.uniqueIndex(parent.getImportedGroups(), STGroup::getName);

        final STGroup testGroup = children.get("postgres_" + version + ".sql");
        assertThat(testGroup).withFailMessage("Could not load test group for postgres %s", version).isNotNull();
        testGroup.load();

        // sanity check that all tests also have results. This also catches parse errors in the query templates
        Set<String> templateNames = testGroup.getTemplateNames();
        for (String name : templateNames) {
            if (name.endsWith("Result")) {
                assertThat(templateNames).contains(name.substring(0, name.length() - 6));
            } else {
                assertThat(templateNames).contains(name + "Result");
            }
        }

        // JUnit5 is awesome
        return templateNames.stream()
                .map(s -> s.substring(1))
                .filter(s -> !s.endsWith("Result"))
                .map(s -> DynamicTest.dynamicTest(s, () -> assertThat(
                        pgExtension.getSharedHandle()
                                .createQuery(s)
                                .mapTo(String.class)
                                .one())
                        .isEqualTo(getResult(testGroup, s))));
    }


    private String getResult(STGroup testGroup, String queryName) {
        final ST template = testGroup.getInstanceOf(queryName + "Result");
        assertThat(template).withFailMessage("Result template for query %s was not found!", queryName).isNotNull();
        return template.render();
    }

    public static class StringTemplateLocatorPlugin implements JdbiPlugin {

        private final STGroup stGroup;
        private final TemplateEngine templateEngine;

        public StringTemplateLocatorPlugin(Class<?> clazz) {
            this.stGroup = StringTemplateSqlLocator.findStringTemplateGroup(clazz);

            this.templateEngine = (templateName, ctx) -> {
                final ST template = stGroup.getInstanceOf(templateName);
                if (template == null) {
                    return templateName;
                }

                ctx.getAttributes().forEach(template::add);
                return template.render();
            };
        }

        @Override
        public void customizeJdbi(Jdbi jdbi) {
            jdbi.getConfig(SqlStatements.class).setTemplateEngine(templateEngine);
        }
    }
}

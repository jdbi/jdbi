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
package org.jdbi.v3.postgres;

import java.util.List;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.BindListStyle;
import org.jdbi.v3.core.statement.EmptyHandling;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestBindListStyle {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withPlugins(new PostgresPlugin())
        .withInitializer((ds, h) -> h.useTransaction(th -> {
            th.execute("CREATE TABLE links (link TEXT PRIMARY KEY)");
            th.execute("INSERT INTO links (link) VALUES ('one.com'), ('two.com')");
        }));

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = pgExtension.openHandle();
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    @Test
    public void testInValuesList() {
        List<String> links = handle.createQuery("SELECT link FROM links WHERE link IN (VALUES <links>) ORDER BY link")
            .configure(SqlStatements.class, c -> c.setBindListStyle(BindListStyle.ROWS))
            .bindList("links", "two.com", "three.com", "one.com")
            .mapTo(String.class)
            .list();

        assertThat(links).containsExactly("one.com", "two.com");
    }

    @Test
    public void testValuesAsTable() {
        List<String> newLinks = handle.createQuery(
                "SELECT link FROM (VALUES <links>) AS newlinks (link) EXCEPT SELECT link FROM links")
            .configure(SqlStatements.class, c -> c.setBindListStyle(BindListStyle.ROWS))
            .bindList("links", "one.com", "three.com")
            .mapTo(String.class)
            .list();

        assertThat(newLinks).containsExactly("three.com");
    }

    @Test
    public void testNullKeywordIsInvalidWithRows() {
        assertThatThrownBy(() -> handle.createQuery("SELECT link FROM links WHERE link IN (VALUES <links>)")
                .configure(SqlStatements.class, c -> c.setBindListStyle(BindListStyle.ROWS))
                .bindList(EmptyHandling.NULL_KEYWORD, "links", List.of())
                .mapTo(String.class)
                .list())
            .isInstanceOf(UnableToExecuteStatementException.class)
            .hasMessageContaining("syntax error");
    }
}

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

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import de.softwareforge.testing.postgres.junit5.RequirePostgresVersion;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

@RequirePostgresVersion(atLeast = "10")
public class TestJsonOperator {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withPlugins(new SqlObjectPlugin(), new PostgresPlugin());

    /**
     * Examples from <a href="https://www.postgresql.org/docs/current/static/functions-json.html">Postgres JSON Functions</a>. Escaping rules from <a
     * href="https://jdbc.postgresql.org/documentation/head/statement.html">Postgres Prepared Statements</a>.
     */
    @Test
    public void testJsonQuery() {
        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '[{\"a\":\"foo\"},{\"b\":\"bar\"},{\"c\":\"baz\"}]'::json->2")
            .mapTo(String.class)
            .one())
            .isEqualTo("{\"c\":\"baz\"}");

        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '{\"a\": {\"b\":\"foo\"}}'::json->'a'")
            .mapTo(String.class)
            .one())
            .isEqualTo("{\"b\":\"foo\"}");

        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '[1,2,3]'::json->>2")
            .mapTo(Integer.class)
            .one())
            .isEqualTo(3);

        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '{\"a\":1,\"b\":2}'::json->>'b'")
            .mapTo(Integer.class)
            .one())
            .isEqualTo(2);

        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '{\"a\": {\"b\":{\"c\": \"foo\"}}}'::json#>'{a,b}'")
            .mapTo(String.class)
            .one())
            .isEqualTo("{\"c\": \"foo\"}");

        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '{\"a\":[1,2,3],\"b\":[4,5,6]}'::json#>>'{a,2}'")
            .mapTo(Integer.class)
            .one())
            .isEqualTo(3);

        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '{\"a\":1, \"b\":2}'::jsonb @> '{\"b\":2}'::jsonb")
            .mapTo(boolean.class)
            .one())
            .isTrue();

        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '{\"b\":2}'::jsonb <@ '{\"a\":1, \"b\":2}'::jsonb")
            .mapTo(boolean.class)
            .one())
            .isTrue();

        // ? escaped to ??
        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '{\"a\":1, \"b\":2}'::jsonb ?? 'b'")
            .mapTo(boolean.class)
            .one())
            .isTrue();

        // ?| escaped to ??|
        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '{\"a\":1, \"b\":2, \"c\":3}'::jsonb ??| array['b', 'c']")
            .mapTo(boolean.class)
            .one())
            .isTrue();

        // ?& escaped to ??&
        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '[\"a\", \"b\"]'::jsonb ??& array['a', 'b']")
            .mapTo(boolean.class)
            .one())
            .isTrue();

        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '[\"a\", \"b\"]'::jsonb || '[\"c\", \"d\"]'::jsonb")
            .mapTo(String.class)
            .one())
            .isEqualTo("[\"a\", \"b\", \"c\", \"d\"]");

        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '{\"a\": \"b\"}'::jsonb - 'a'")
            .mapTo(String.class)
            .one())
            .isEqualTo("{}");

        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '{\"a\": \"b\", \"c\": \"d\"}'::jsonb - '{a,c}'::text[]")
            .mapTo(String.class)
            .one())
            .isEqualTo("{}");

        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '[\"a\", \"b\"]'::jsonb - 1")
            .mapTo(String.class)
            .one())
            .isEqualTo("[\"a\"]");

        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '[\"a\", {\"b\":1}]'::jsonb #- '{1,b}'")
            .mapTo(String.class)
            .one())
            .isEqualTo("[\"a\", {}]");
    }

    @Test
    public void testJsonQueryWithBindedInput() {
        assertThat(pgExtension.openHandle()
            .createQuery("SELECT '{\"a\":1, \"b\":2}'::jsonb ?? :key")
            .bind("key", "a")
            .mapTo(boolean.class)
            .one())
            .isTrue();
    }
}

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
package org.jdbi.v3.sqlobject;

import com.google.common.collect.ImmutableSet;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.PgDatabaseRule;
import org.jdbi.v3.postgres.PostgresDbRule;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TestJsonOperator {
    @Rule
    public JdbiRule db = PostgresDbRule.rule();

    @Rule
    public PgDatabaseRule dbRule = new PgDatabaseRule()
        .withPlugin(new SqlObjectPlugin())
        .withPlugin(new PostgresPlugin());
    private Handle handle;
    private KeyValueStore kvs;

    @Before
    public void setUp() throws Exception {
        handle = dbRule.openHandle();
        handle.execute("create table something (id serial primary key, value jsonb)");
        kvs = handle.attach(KeyValueStore.class);
    }

    @Test
    public void testHasProperty() throws Exception {
        kvs.insert(1, "{\"a\":1, \"b\":2}");
        assertThat(kvs.hasProperty("a")).isEqualTo(ImmutableSet.of(1));
        assertThat(kvs.hasProperty("b")).isEqualTo(ImmutableSet.of(1));
        assertThat(kvs.hasProperty("c")).isEqualTo(Collections.emptySet());
    }

    public interface KeyValueStore {
        @SqlUpdate("insert into something (id, value) values (:id, cast(:value as jsonb))")
        void insert(@Bind("id") int id, @Bind("value") String value);

        @SqlQuery("select id from something where value ?? :property")
        Set<Integer> hasProperty(@Bind("property") String property);
    }
}

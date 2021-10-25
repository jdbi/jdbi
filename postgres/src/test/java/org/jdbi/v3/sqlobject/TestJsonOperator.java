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

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestJsonOperator {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withPlugins(new SqlObjectPlugin(), new PostgresPlugin())
        .withInitializer((ds, h) -> {
            h.execute("create table something (id serial primary key, value jsonb)");
        });

    private KeyValueStore kvs;

    @BeforeEach
    public void setUp() {
        kvs = pgExtension.attach(KeyValueStore.class);
    }

    @Test
    public void testHasProperty() {
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

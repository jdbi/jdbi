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
import java.util.Map;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.immutables.value.Value;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.immutables.JdbiImmutables;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.BindPojo;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestImmutablesHStore {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults()
        .withDatabasePreparer(ds -> Jdbi.create(ds).withHandle(h -> h.execute("create extension hstore")))
        .build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withPlugins(new SqlObjectPlugin(), new PostgresPlugin())
        .withConfig(JdbiImmutables.class, c -> c.registerImmutable(Mappy.class))
        .withInitializer((ds, h) -> h.execute("create table mappy (numbers hstore not null)"));

    MappyDao dao;

    @BeforeEach
    public void setup() {
        dao = pgExtension.attach(MappyDao.class);
    }

    @Value.Immutable
    @Value.Style(overshadowImplementation = true)
    public interface Mappy {

        @HStore
        Map<String, String> numbers();

        class Builder extends ImmutableMappy.Builder {}

        static Mappy.Builder builder() {
            return new Builder();
        }
    }

    @Test
    public void testMap() {
        final Mappy row1 = Mappy.builder()
                .putNumbers("one", "1")
                .putNumbers("two", "2")
                .build();
        dao.insert(row1);

        final Mappy row2 = Mappy.builder()
                .putNumbers("three", "3")
                .putNumbers("four", "4")
                .putNumbers("five", "5")
                .build();
        dao.insert(row2);

        assertThat(dao.select())
            .containsExactlyInAnyOrder(row1, row2);
    }

    public interface MappyDao {
        @SqlUpdate("insert into mappy (numbers) values (:numbers)")
        int insert(@BindPojo Mappy mappy);

        @SqlQuery("select numbers from mappy")
        List<Mappy> select();
    }
}

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

import java.time.Instant;
import java.util.Optional;

import lombok.Data;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.time.temporal.ChronoUnit.SECONDS;

import static org.assertj.core.api.Assertions.assertThat;

public class TestInheritedAnnotations {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private MockClock mockClock = new MockClock();

    @Before
    public void setUp() {
        dbRule.getJdbi().getConfig(BindTime.Config.class).clock = mockClock;

        Handle handle = dbRule.getSharedHandle();
        handle.execute("CREATE TABLE characters (id INT, name VARCHAR, created TIMESTAMP, modified TIMESTAMP)");
    }

    @Test
    public void testCrud() {
        Instant inserted = mockClock.instant();

        CharacterDao dao = dbRule.getJdbi().onDemand(CharacterDao.class);

        dao.insert(new Character(1, "Moiraine Sedai"));

        assertThat(dao.findById(1)).contains(new Character(1, "Moiraine Sedai", inserted, inserted));

        Instant modified = mockClock.advance(10, SECONDS);
        assertThat(inserted).isBefore(modified);

        dao.update(new Character(1, "Mistress Alys"));

        assertThat(dao.findById(1)).contains(new Character(1, "Mistress Alys", inserted, modified));

        dao.delete(1);
        assertThat(dao.findById(1)).isEmpty();
    }

    @UseClasspathSqlLocator // configuring annotation
    @BindTime // sql statement customizing annotation
    public interface CrudDao<T, ID> {
        @SqlUpdate
        void insert(@BindBean T entity);

        @SqlQuery
        Optional<T> findById(ID id);

        @SqlUpdate
        void update(@BindBean T entity);

        @SqlUpdate
        void delete(ID id);
    }

    @RegisterConstructorMapper(Character.class)
    public interface CharacterDao extends CrudDao<Character, Integer> {}

    @Data
    public static class Character {
        public final int id;
        public final String name;
        private final Instant created;
        private final Instant modified;

        public Character(int id, String name) {
            this(id, name, null, null);
        }

        @JdbiConstructor
        public Character(int id, String name, Instant created, Instant modified) {
            this.id = id;
            this.name = name;
            this.created = created;
            this.modified = modified;
        }
    }
}

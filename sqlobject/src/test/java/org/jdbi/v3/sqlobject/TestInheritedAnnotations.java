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

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
  public void testCrud() throws Exception {
    Instant inserted = mockClock.instant();

    CharacterDao dao = dbRule.getJdbi().onDemand(CharacterDao.class);

    dao.insert(new Character(1, "Moiraine Sedai"));

    assertThat(dao.findById(1)).contains(new Character(1, "Moiraine Sedai", inserted, inserted));

    Instant modified = mockClock.advance(10, SECONDS);
    assertThat(inserted).isLessThan(modified);

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

    public int getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public Instant getCreated() {
      return created;
    }

    public Instant getModified() {
      return modified;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
          return true;
      }
      if (o == null || getClass() != o.getClass()) {
          return false;
      }
      Character character = (Character) o;
      return id == character.id &&
          Objects.equals(name, character.name) &&
          Objects.equals(created, character.created) &&
          Objects.equals(modified, character.modified);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, name, created, modified);
    }

    @Override
    public String toString() {
      return "Character{" +
          "id=" + id +
          ", name='" + name + '\'' +
          ", created=" + created +
          ", modified=" + modified +
          '}';
    }
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @SqlStatementCustomizingAnnotation(BindTime.Factory.class)
  public @interface BindTime {

    class Factory implements SqlStatementCustomizerFactory {
      @Override
      public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType) {
        return stmt -> stmt.bind("now", OffsetDateTime.now(stmt.getConfig(Config.class).clock));
      }
    }

    class Config implements JdbiConfig<Config> {
      public Clock clock;

      @Override
      public Config createCopy() {
        Config copy = new Config();
        copy.clock = this.clock;
        return copy;
      }
    }
  }
}

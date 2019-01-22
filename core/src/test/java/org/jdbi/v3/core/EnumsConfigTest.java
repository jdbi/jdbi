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
package org.jdbi.v3.core;

import java.lang.annotation.RetentionPolicy;

import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.rule.SqliteDatabaseRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EnumsConfigTest {
    @Rule
    public SqliteDatabaseRule db = new SqliteDatabaseRule();

    @Test
    public void byNameIsDefault() {
        assertThat(db.getJdbi().getConfig(Enums.class).getDefaultStrategy())
            .isEqualTo(Enums.EnumStrategy.BY_NAME);
    }

    @Test
    public void namesAreBoundCorrectly() {
        db.getJdbi().useHandle(h -> {
            h.createUpdate("create table enums(id int, name varchar)").execute();

            h.createUpdate("insert into enums (id, name) values (1, :name)")
                .bind("name", Foobar.FOO)
                .execute();

            String ordinal = h.createQuery("select name from enums")
                .mapTo(String.class)
                .findOnly();

            assertThat(ordinal)
                .isEqualTo(Foobar.FOO.name());
        });
    }

    @Test
    public void namesAreMappedCorrectly() {
        db.getJdbi().useHandle(h -> {
            Foobar name = h.createQuery("select :name")
                .bind("name", Foobar.FOO.name())
                .mapTo(Foobar.class)
                .findOnly();

            assertThat(name)
                .isEqualTo(Foobar.FOO);
        });
    }

    @Test
    public void ordinalsAreBoundCorrectly() {
        db.getJdbi().useHandle(h -> {
            h.getConfig(Enums.class).defaultByOrdinal();

            h.createUpdate("create table enums(id int, ordinal int)").execute();

            h.createUpdate("insert into enums (id, ordinal) values (1, :ordinal)")
                .bind("ordinal", Foobar.FOO)
                .execute();

            Integer ordinal = h.createQuery("select ordinal from enums")
                .mapTo(Integer.class)
                .findOnly();

            assertThat(ordinal)
                .isEqualTo(Foobar.FOO.ordinal());
        });
    }

    @Test
    public void ordinalsAreMappedCorrectly() {
        db.getJdbi().useHandle(h -> {
            h.getConfig(Enums.class).defaultByOrdinal();

            Foobar name = h.createQuery("select :ordinal")
                .bind("ordinal", Foobar.FOO.ordinal())
                .mapTo(Foobar.class)
                .findOnly();

            assertThat(name)
                .isEqualTo(Foobar.FOO);
        });
    }

    @Test
    public void badNameThrows() {
        db.getJdbi().useHandle(h -> {
            assertThatThrownBy(h.createQuery("select 'xxx'").mapTo(Foobar.class)::findOnly)
                .isInstanceOf(UnableToProduceResultException.class)
                .hasMessageContaining("no Foobar value could be matched to the name xxx");
        });
    }

    @Test
    public void badOrdinalThrows() {
        db.getJdbi().useHandle(h -> {
            h.getConfig(Enums.class).defaultByOrdinal();

            assertThatThrownBy(h.createQuery("select 2").mapTo(Foobar.class)::findOnly)
                .isInstanceOf(UnableToProduceResultException.class)
                .hasMessageContaining("no Foobar value could be matched to the ordinal 2");
        });
    }

    @Test
    public void testNull() {
        db.getJdbi().useHandle(h -> {
            h.createUpdate("create table enums(value varchar)").execute();

            h.createUpdate("insert into enums(value) values(:enum)")
                .bindByType("enum", null, Foobar.class)
                .execute();

            String inserted = h.createQuery("select value from enums")
                .mapTo(String.class)
                .findOnly();
            assertThat(inserted).isNull();

            Foobar mapped = h.createQuery("select value from enums")
                .mapTo(Foobar.class)
                .findOnly();
            assertThat(mapped).isNull();
        });
    }

    // bar is unused to make sure we don't have any coincidental correctness
    private enum Foobar {
        BAR, FOO
    }

    @Test
    public void testConflictingQualifiers() {
        QualifiedType<RetentionPolicy> type = QualifiedType.of(RetentionPolicy.class).with(EnumByName.class, EnumByOrdinal.class);

        assertThatThrownBy(() -> db.getJdbi().getConfig(Enums.class).findStrategy(type, RetentionPolicy.class))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConflictingSourceAnnotations() {
        assertThatThrownBy(() -> db.getJdbi().getConfig(Enums.class).findStrategy(QualifiedType.of(BiPolar.class), BiPolar.class))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @EnumByName
    @EnumByOrdinal
    public enum BiPolar {
        BEAR
    }
}

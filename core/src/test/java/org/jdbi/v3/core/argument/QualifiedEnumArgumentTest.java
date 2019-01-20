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
package org.jdbi.v3.core.argument;

import org.jdbi.v3.core.EnumByName;
import org.jdbi.v3.core.EnumByOrdinal;
import org.jdbi.v3.core.Enums;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.rule.SqliteDatabaseRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QualifiedEnumArgumentTest {
    @Rule
    public SqliteDatabaseRule db = new SqliteDatabaseRule();

    @Test
    public void methodCallCanBeAnnotatedAsByName() {
        db.getJdbi().useHandle(h -> {
            h.getConfig(Enums.class).defaultByOrdinal();

            h.createUpdate("create table enums(id int, name varchar)").execute();

            h.createUpdate("insert into enums (id, name) values (1, :name)")
                .bindByType("name", Foobar.FOO, QualifiedType.of(Foobar.class).with(EnumByName.class))
                .execute();

            String inserted = h.createQuery("select name from enums")
                .mapTo(String.class)
                .findOnly();
            assertThat(inserted).isEqualTo(Foobar.FOO.name());
        });
    }

    @Test
    public void methodCallCanBeAnnotatedAsByOrdinal() {
        db.getJdbi().useHandle(h -> {
            h.createUpdate("create table enums(id int, ordinal int)").execute();

            h.createUpdate("insert into enums (id, ordinal) values (1, :ordinal)")
                .bindByType("ordinal", Foobar.FOO, QualifiedType.of(Foobar.class).with(EnumByOrdinal.class))
                .execute();

            Integer inserted = h.createQuery("select ordinal from enums")
                .mapTo(Integer.class)
                .findOnly();
            assertThat(inserted).isEqualTo(Foobar.FOO.ordinal());
        });
    }

    @Test
    public void enumCanBeAnnotatedAsByName() {
        db.getJdbi().useHandle(h -> {
            h.getConfig(Enums.class).defaultByOrdinal();

            h.createUpdate("create table enums(id int, name varchar)").execute();

            h.createUpdate("insert into enums(id, name) values(1, :name)")
                .bind("name", ByName.ALPHABETIC)
                .execute();

            String inserted = h.createQuery("select name from enums")
                .mapTo(String.class)
                .findOnly();
            assertThat(inserted).isEqualTo(ByName.ALPHABETIC.name());
        });
    }

    @Test
    public void enumCanBeAnnotatedAsByOrdinal() {
        db.getJdbi().useHandle(h -> {
            h.createUpdate("create table enums(id int, ordinal int)").execute();

            h.createUpdate("insert into enums(id, ordinal) values(1, :ordinal)")
                .bind("ordinal", ByOrdinal.NUMERIC)
                .execute();

            Integer inserted = h.createQuery("select ordinal from enums")
                .mapTo(Integer.class)
                .findOnly();
            assertThat(inserted).isEqualTo(ByOrdinal.NUMERIC.ordinal());
        });
    }

    @Test
    public void methodCallOverridesClassForName() {
        db.getJdbi().useHandle(h -> {
            h.getConfig(Enums.class).defaultByOrdinal();

            h.createUpdate("create table enums(id int, name varchar)").execute();

            h.createUpdate("insert into enums(id, name) values(1, :name)")
                .bindByType("name", ByOrdinal.NUMERIC, QualifiedType.of(ByOrdinal.class).with(EnumByName.class))
                .execute();

            String inserted = h.createQuery("select name from enums")
                .mapTo(String.class)
                .findOnly();
            assertThat(inserted).isEqualTo(ByOrdinal.NUMERIC.name());
        });
    }

    @Test
    public void methodCallOverridesClassForOrdinal() {
        db.getJdbi().useHandle(h -> {
            h.createUpdate("create table enums(id int, ordinal int)").execute();

            h.createUpdate("insert into enums(id, ordinal) values(1, :ordinal)")
                .bindByType("ordinal", ByName.ALPHABETIC, QualifiedType.of(ByName.class).with(EnumByOrdinal.class))
                .execute();

            Integer inserted = h.createQuery("select ordinal from enums")
                .mapTo(Integer.class)
                .findOnly();
            assertThat(inserted).isEqualTo(ByName.ALPHABETIC.ordinal());
        });
    }

    // bar is unused to make sure we don't have any coincidental correctness
    private enum Foobar {
        BAR, FOO
    }

    @EnumByName
    private enum ByName {
        BAR, ALPHABETIC
    }

    @EnumByOrdinal
    private enum ByOrdinal {
        BAR, NUMERIC
    }
}

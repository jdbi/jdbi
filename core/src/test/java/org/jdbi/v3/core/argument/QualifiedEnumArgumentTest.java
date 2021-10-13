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

import org.jdbi.v3.core.enums.EnumByName;
import org.jdbi.v3.core.enums.EnumByOrdinal;
import org.jdbi.v3.core.enums.EnumStrategy;
import org.jdbi.v3.core.enums.Enums;
import org.jdbi.v3.core.junit5.DatabaseExtension;
import org.jdbi.v3.core.junit5.SqliteDatabaseExtension;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class QualifiedEnumArgumentTest {
    @RegisterExtension
    public DatabaseExtension sqliteExtension = SqliteDatabaseExtension.instance();

    @Test
    public void methodCallCanBeAnnotatedAsByName() {
        sqliteExtension.getJdbi().useHandle(h -> {
            h.getConfig(Enums.class).setEnumStrategy(EnumStrategy.BY_ORDINAL);

            h.createUpdate("create table enums(id int, name varchar)").execute();

            h.createUpdate("insert into enums (id, name) values (1, :name)")
                .bindByType("name", Foobar.FOO, QualifiedType.of(Foobar.class).with(EnumByName.class))
                .execute();

            String inserted = h.createQuery("select name from enums")
                .mapTo(String.class)
                .one();
            assertThat(inserted).isEqualTo(Foobar.FOO.name());
        });
    }

    @Test
    public void methodCallCanBeAnnotatedAsByOrdinal() {
        sqliteExtension.getJdbi().useHandle(h -> {
            h.createUpdate("create table enums(id int, ordinal int)").execute();

            h.createUpdate("insert into enums (id, ordinal) values (1, :ordinal)")
                .bindByType("ordinal", Foobar.FOO, QualifiedType.of(Foobar.class).with(EnumByOrdinal.class))
                .execute();

            Integer inserted = h.createQuery("select ordinal from enums")
                .mapTo(Integer.class)
                .one();
            assertThat(inserted).isEqualTo(Foobar.FOO.ordinal());
        });
    }

    @Test
    public void enumCanBeAnnotatedAsByName() {
        sqliteExtension.getJdbi().useHandle(h -> {
            h.getConfig(Enums.class).setEnumStrategy(EnumStrategy.BY_ORDINAL);

            h.createUpdate("create table enums(id int, name varchar)").execute();

            h.createUpdate("insert into enums(id, name) values(1, :name)")
                .bind("name", ByName.ALPHABETIC)
                .execute();

            String inserted = h.createQuery("select name from enums")
                .mapTo(String.class)
                .one();
            assertThat(inserted).isEqualTo(ByName.ALPHABETIC.name());
        });
    }

    @Test
    public void enumCanBeAnnotatedAsByOrdinal() {
        sqliteExtension.getJdbi().useHandle(h -> {
            h.createUpdate("create table enums(id int, ordinal int)").execute();

            h.createUpdate("insert into enums(id, ordinal) values(1, :ordinal)")
                .bind("ordinal", ByOrdinal.NUMERIC)
                .execute();

            Integer inserted = h.createQuery("select ordinal from enums")
                .mapTo(Integer.class)
                .one();
            assertThat(inserted).isEqualTo(ByOrdinal.NUMERIC.ordinal());
        });
    }

    @Test
    public void methodCallOverridesClassForName() {
        sqliteExtension.getJdbi().useHandle(h -> {
            h.getConfig(Enums.class).setEnumStrategy(EnumStrategy.BY_ORDINAL);

            h.createUpdate("create table enums(id int, name varchar)").execute();

            h.createUpdate("insert into enums(id, name) values(1, :name)")
                .bindByType("name", ByOrdinal.NUMERIC, QualifiedType.of(ByOrdinal.class).with(EnumByName.class))
                .execute();

            String inserted = h.createQuery("select name from enums")
                .mapTo(String.class)
                .one();
            assertThat(inserted).isEqualTo(ByOrdinal.NUMERIC.name());
        });
    }

    @Test
    public void methodCallOverridesClassForOrdinal() {
        sqliteExtension.getJdbi().useHandle(h -> {
            h.createUpdate("create table enums(id int, ordinal int)").execute();

            h.createUpdate("insert into enums(id, ordinal) values(1, :ordinal)")
                .bindByType("ordinal", ByName.ALPHABETIC, QualifiedType.of(ByName.class).with(EnumByOrdinal.class))
                .execute();

            Integer inserted = h.createQuery("select ordinal from enums")
                .mapTo(Integer.class)
                .one();
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

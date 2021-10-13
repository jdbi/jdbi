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
package org.jdbi.v3.core.mapper;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.enums.EnumByName;
import org.jdbi.v3.core.enums.EnumByOrdinal;
import org.jdbi.v3.core.enums.EnumStrategy;
import org.jdbi.v3.core.enums.Enums;
import org.jdbi.v3.core.junit5.DatabaseExtension;
import org.jdbi.v3.core.junit5.SqliteDatabaseExtension;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class QualifiedEnumMappingTest {
    @RegisterExtension
    public DatabaseExtension sqliteExtension = SqliteDatabaseExtension.instance();

    private Handle h;

    @BeforeEach
    public void before() {
        h = sqliteExtension.getSharedHandle();
    }

    @Test
    public void methodCallCanBeAnnotatedAsByName() {
        h.getConfig(Enums.class).setEnumStrategy(EnumStrategy.BY_ORDINAL);

        Object byName = h.createQuery("select :name")
            .bind("name", Foobar.FOO.name())
            .mapTo(QualifiedType.of(Foobar.class).with(EnumByName.class))
            .one();

        assertThat(byName)
            .isEqualTo(Foobar.FOO);
    }

    @Test
    public void methodCallCanBeAnnotatedAsByOrdinal() {
        Object byOrdinal = h.createQuery("select :ordinal")
            .bind("ordinal", Foobar.FOO.ordinal())
            .mapTo(QualifiedType.of(Foobar.class).with(EnumByOrdinal.class))
            .one();

        assertThat(byOrdinal)
            .isEqualTo(Foobar.FOO);
    }

    @Test
    public void enumCanBeAnnotatedAsByName() {
        h.getConfig(Enums.class).setEnumStrategy(EnumStrategy.BY_ORDINAL);

        ByName byName = h.createQuery("select :name")
            .bind("name", ByName.ALPHABETIC.name())
            .mapTo(ByName.class)
            .one();

        assertThat(byName)
            .isEqualTo(ByName.ALPHABETIC);
    }

    @Test
    public void enumCanBeAnnotatedAsByOrdinal() {
        ByOrdinal byOrdinal = h.createQuery("select :ordinal")
            .bind("ordinal", ByOrdinal.NUMERIC.ordinal())
            .mapTo(ByOrdinal.class)
            .one();

        assertThat(byOrdinal)
            .isEqualTo(ByOrdinal.NUMERIC);
    }

    @Test
    public void methodCallOverridesClassForName() {
        h.getConfig(Enums.class).setEnumStrategy(EnumStrategy.BY_ORDINAL);

        Object byName = h.createQuery("select :name")
            .bind("name", ByOrdinal.NUMERIC.name())
            .mapTo(QualifiedType.of(ByOrdinal.class).with(EnumByName.class))
            .one();

        assertThat(byName)
            .isEqualTo(ByOrdinal.NUMERIC);
    }

    @Test
    public void methodCallOverridesClassForOrdinal() {
        Object byOrdinal = h.createQuery("select :ordinal")
            .bind("ordinal", ByName.ALPHABETIC.ordinal())
            .mapTo(QualifiedType.of(ByName.class).with(EnumByOrdinal.class))
            .one();

        assertThat(byOrdinal)
            .isEqualTo(ByName.ALPHABETIC);
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

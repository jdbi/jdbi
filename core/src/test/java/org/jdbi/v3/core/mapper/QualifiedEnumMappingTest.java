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
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.rule.SqliteDatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QualifiedEnumMappingTest {
    @Rule
    public SqliteDatabaseRule db = new SqliteDatabaseRule();

    private Handle h;

    @Before
    public void before() {
        h = db.getSharedHandle();
    }

    @Test
    public void methodCallCanBeAnnotatedAsByName() {
        h.getConfig(Enums.class).setEnumStrategy(EnumStrategy.BY_ORDINAL);

        Object byName = h.createQuery("select :name")
            .bind("name", Foobar.FOO.name())
            .mapTo(QualifiedType.of(Foobar.class).with(EnumByName.class))
            .findOnly();

        assertThat(byName)
            .isEqualTo(Foobar.FOO);
    }

    @Test
    public void methodCallCanBeAnnotatedAsByOrdinal() {
        Object byOrdinal = h.createQuery("select :ordinal")
            .bind("ordinal", Foobar.FOO.ordinal())
            .mapTo(QualifiedType.of(Foobar.class).with(EnumByOrdinal.class))
            .findOnly();

        assertThat(byOrdinal)
            .isEqualTo(Foobar.FOO);
    }

    @Test
    public void enumCanBeAnnotatedAsByName() {
        h.getConfig(Enums.class).setEnumStrategy(EnumStrategy.BY_ORDINAL);

        ByName byName = h.createQuery("select :name")
            .bind("name", ByName.ALPHABETIC.name())
            .mapTo(ByName.class)
            .findOnly();

        assertThat(byName)
            .isEqualTo(ByName.ALPHABETIC);
    }

    @Test
    public void enumCanBeAnnotatedAsByOrdinal() {
        ByOrdinal byOrdinal = h.createQuery("select :ordinal")
            .bind("ordinal", ByOrdinal.NUMERIC.ordinal())
            .mapTo(ByOrdinal.class)
            .findOnly();

        assertThat(byOrdinal)
            .isEqualTo(ByOrdinal.NUMERIC);
    }

    @Test
    public void methodCallOverridesClassForName() {
        h.getConfig(Enums.class).setEnumStrategy(EnumStrategy.BY_ORDINAL);

        Object byName = h.createQuery("select :name")
            .bind("name", ByOrdinal.NUMERIC.name())
            .mapTo(QualifiedType.of(ByOrdinal.class).with(EnumByName.class))
            .findOnly();

        assertThat(byName)
            .isEqualTo(ByOrdinal.NUMERIC);
    }

    @Test
    public void methodCallOverridesClassForOrdinal() {
        Object byOrdinal = h.createQuery("select :ordinal")
            .bind("ordinal", ByName.ALPHABETIC.ordinal())
            .mapTo(QualifiedType.of(ByName.class).with(EnumByOrdinal.class))
            .findOnly();

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

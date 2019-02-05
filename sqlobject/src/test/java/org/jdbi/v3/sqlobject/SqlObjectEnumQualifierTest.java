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

import org.jdbi.v3.core.enums.EnumByName;
import org.jdbi.v3.core.enums.EnumByOrdinal;
import org.jdbi.v3.core.enums.EnumStrategy;
import org.jdbi.v3.core.enums.Enums;
import org.jdbi.v3.sqlobject.config.UseEnumStrategy;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SqlObjectEnumQualifierTest {
    @Rule
    public JdbiRule db = JdbiRule.sqlite().withPlugin(new SqlObjectPlugin());

    @Test
    public void byOrdinalOverridesDefaultInBindingAndMapping() {
        db.getJdbi().useHandle(h -> {
            h.execute("create table enums(ordinal int)");

            FooByOrdinalDao dao = h.attach(FooByOrdinalDao.class);

            dao.insert(Foo.BAR);
            assertThat(h.createQuery("select ordinal from enums").mapTo(Integer.class).findOnly()).isEqualTo(0);

            Foo value = dao.select();
            assertThat(value).isEqualTo(Foo.BAR);
        });
    }

    @Test
    public void byNameOverridesDefaultInBindingAndMapping() {
        db.getJdbi().useHandle(h -> {
            h.getConfig(Enums.class).setEnumStrategy(EnumStrategy.BY_ORDINAL);

            h.execute("create table enums(name varchar)");

            FooByNameDao dao = h.attach(FooByNameDao.class);

            dao.insert(Foo.BAR);
            assertThat(h.createQuery("select name from enums").mapTo(String.class).findOnly()).isEqualTo("BAR");

            Foo value = dao.select();
            assertThat(value).isEqualTo(Foo.BAR);
        });
    }

    @Test
    public void useEnumStrategyOrdinalAnnotation() {
        db.getJdbi().useHandle(h -> {
            h.getConfig(Enums.class).setEnumStrategy(EnumStrategy.BY_NAME); // dao annotations will override

            h.execute("create table enums(ordinal int)");

            UseEnumStrategyOrdinalDao dao = h.attach(UseEnumStrategyOrdinalDao.class);

            dao.insert(Foo.BAR);
            assertThat(h.createQuery("select ordinal from enums").mapTo(Integer.class).findOnly()).isEqualTo(0);

            Foo value = dao.select();
            assertThat(value).isEqualTo(Foo.BAR);
        });
    }

    @Test
    public void useEnumStrategyNameAnnotation() {
        db.getJdbi().useHandle(h -> {
            h.getConfig(Enums.class).setEnumStrategy(EnumStrategy.BY_ORDINAL); // dao annotations will override

            h.execute("create table enums(name varchar)");

            UseEnumStrategyNameDao dao = h.attach(UseEnumStrategyNameDao.class);

            dao.insert(Foo.BAR);
            assertThat(h.createQuery("select name from enums").mapTo(String.class).findOnly()).isEqualTo("BAR");

            Foo value = dao.select();
            assertThat(value).isEqualTo(Foo.BAR);
        });
    }

    public enum Foo {
        BAR
    }

    private interface FooByOrdinalDao {
        @SqlUpdate("insert into enums(ordinal) values(:value)")
        void insert(@EnumByOrdinal Foo value);

        @SqlQuery("select ordinal from enums")
        @EnumByOrdinal
        Foo select();
    }

    private interface FooByNameDao {
        @SqlUpdate("insert into enums(name) values(:value)")
        void insert(@EnumByName Foo value);

        @SqlQuery("select name from enums")
        @EnumByName
        Foo select();
    }

    @UseEnumStrategy(EnumStrategy.BY_ORDINAL)
    private interface UseEnumStrategyOrdinalDao {
        @SqlUpdate("insert into enums(ordinal) values(:value)")
        void insert(Foo value);

        @SqlQuery("select ordinal from enums")
        Foo select();
    }

    @UseEnumStrategy(EnumStrategy.BY_NAME)
    private interface UseEnumStrategyNameDao {
        @SqlUpdate("insert into enums(name) values(:value)")
        void insert(Foo value);

        @SqlQuery("select name from enums")
        Foo select();
    }

}

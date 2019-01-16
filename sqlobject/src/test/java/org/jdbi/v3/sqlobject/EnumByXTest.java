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

import org.jdbi.v3.core.EnumByOrdinal;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EnumByXTest {
    @Rule
    public JdbiRule db = JdbiRule.sqlite().withPlugin(new SqlObjectPlugin());

    @Test
    public void annotationOverridesDefaultInBindingAndMapping() {
        db.getJdbi().useHandle(h -> {
            h.createUpdate("create table enums(ordinal int)").execute();

            FooDao dao = h.attach(FooDao.class);

            dao.insert(Foo.BAR);
            assertThat(h.createQuery("select ordinal from enums").mapTo(Integer.class).findOnly()).isEqualTo(0);

            Foo value = dao.select();
            assertThat(value).isEqualTo(Foo.BAR);
        });
    }

    public enum Foo {
        BAR
    }

    private interface FooDao {
        @SqlUpdate("insert into enums(ordinal) values(:value)")
        void insert(@EnumByOrdinal Foo value);

        @SqlQuery("select ordinal from enums")
        @EnumByOrdinal
        Foo select();
    }
}

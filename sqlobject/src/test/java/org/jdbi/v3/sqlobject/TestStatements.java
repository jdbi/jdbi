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

import static org.assertj.core.api.Assertions.assertThat;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Rule;
import org.junit.Test;

public class TestStatements {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testInsert() throws Exception {
        dbRule.getJdbi().useExtension(Inserter.class, i -> {
            // this is what is under test here
            int rows_affected = i.insert(2, "Diego");

            String name = dbRule.getSharedHandle().createQuery("select name from something where id = 2").mapTo(String.class).findOnly();

            assertThat(rows_affected).isEqualTo(1);
            assertThat(name).isEqualTo("Diego");
        });
    }

    @Test
    public void testInsertWithVoidReturn() throws Exception {
        dbRule.getJdbi().useExtension(Inserter.class, i -> {
            // this is what is under test here
            i.insertWithVoidReturn(2, "Diego");

            String name = dbRule.getSharedHandle().createQuery("select name from something where id = 2").mapTo(String.class).findOnly();

            assertThat(name).isEqualTo("Diego");
        });
    }

    @Test
    public void testDoubleArgumentBind() throws Exception {
        dbRule.getJdbi().useExtension(Doubler.class, d -> assertThat(d.doubleTest("wooooot")).isTrue());
    }

    public interface Inserter {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        int insert(@Bind("id") long id, @Bind("name") String name);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insertWithVoidReturn(@Bind("id") long id, @Bind("name") String name);
    }

    public interface Doubler {
        @SqlQuery("select :test = :test")
        boolean doubleTest(@Bind("test") String test);
    }
}

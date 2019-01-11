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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.TestBindBeanList;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethodsList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestBindMethodsList {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Before
    public void setUp() {
        Handle handle = dbRule.getSharedHandle();
        handle.registerRowMapper(FieldMapper.factory(TestBindBeanList.Thing.class));
        handle.execute("create table thing (id identity primary key, foo varchar(50), bar varchar(50), baz varchar(50))");
    }

    @Test
    public void bindMethodsListWithNoValue() {
        assertThatThrownBy(() -> dbRule.getSharedHandle().createQuery("insert into thing (id, foo, bar, baz) VALUES <items>")
            .bindMethodsList("items", Collections.emptyList(), Arrays.asList("getFoo", "getBar"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void bindMethodsListWithNoMethods() {
        Thing thing = new Thing(1, "foo", "bar", "baz");
        assertThatThrownBy(() -> dbRule.getSharedHandle().createQuery("insert into (id, foo, bar, baz) VALUES <items>")
            .bindMethodsList("items", Collections.singletonList(thing), Collections.emptyList())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void happyPath() {
        Thing thing1 = new Thing(1, "foo1", "bar1", "baz1");
        Thing thing2 = new Thing(2, "foo2", "bar2", "baz2");

        List<Thing> things = Arrays.asList(thing1, thing2);

        final ThingDAO dao = this.dbRule.getJdbi().onDemand(ThingDAO.class);

        assertThat(dao.insert(things)).isEqualTo(things.size());
        assertThat(dao.getBazById(2)).isEqualTo("baz2");
    }

    public interface ThingDAO {
        @SqlUpdate("insert into thing (id, foo, bar, baz) VALUES <items>")
        int insert(@BindMethodsList(value = "items", methodNames = {"getId", "getFoo", "getBar", "getBaz"}) Collection<Thing> thing);

        @SqlQuery("select baz from thing where id = :id")
        String getBazById(@Bind("id") int id);
    }

    @AllArgsConstructor
    public static class Thing {
        @Getter
        private int id;
        private String fooTest;
        private String barTest;
        private String bazTest;

        public String getFoo() {
            return fooTest;
        }

        public String getBar() {
            return barTest;
        }

        public String getBaz() {
            return bazTest;
        }
    }
}

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
package org.jdbi.v3.core.statement;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class TestBindBeanList {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private Handle handle;

    @Before
    public void setUp() {
        handle = db.getSharedHandle();
        handle.registerRowMapper(FieldMapper.of(Thing.class));
        handle.execute("create table thing (id identity primary key, foo varchar(50), bar varchar(50), baz varchar(50))");
        handle.execute("insert into thing (id, foo, bar, baz) values (?, ?, ?, ?)", 1, "foo1", "bar1", "baz1");
        handle.execute("insert into thing (id, foo, bar, baz) values (?, ?, ?, ?)", 2, "foo2", "bar2", "baz2");
        handle.execute("insert into thing (id, foo, bar, baz) values (?, ?, ?, ?)", 3, "foo3", "bar3", "baz3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void bindBeanListWithNoValues() throws Exception {
        handle.createQuery("select id, foo from thing where (foo, bar) in (<keys>)")
                .bindBeanList("keys", Collections.emptyList(), Arrays.asList("foo", "bar"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void bindBeanListWithNoProperties() throws Exception {
        ThingKey thingKey = new ThingKey("a", "b");
        handle.createQuery("select id, foo from thing where (foo, bar) in (<keys>)")
                .bindBeanList("keys", Collections.singletonList(thingKey), Collections.emptyList());
    }

    @Test
    public void happyPath() {
        ThingKey thing1Key = new ThingKey("foo1", "bar1");
        ThingKey thing3Key = new ThingKey("foo3", "bar3");

        List<Thing> list = handle.createQuery("select id, foo from thing where (foo, bar) in (<keys>)")
                .bindBeanList("keys", Arrays.asList(thing1Key, thing3Key), Arrays.asList("foo", "bar"))
                .mapTo(Thing.class)
                .list();
        assertThat(list)
                .extracting(Thing::getId, Thing::getFoo, Thing::getBar, Thing::getBaz)
                .containsExactly(
                        tuple(1, "foo1", null, null),
                        tuple(3, "foo3", null, null));
    }

    public static class ThingKey {
        public String foo;
        public String bar;

        public ThingKey(String foo, String bar) {
            this.foo = foo;
            this.bar = bar;
        }

        public String getFoo() {
            return foo;
        }

        public String getBar() {
            return bar;
        }
    }

    public static class Thing {
        public int id;
        public String foo;
        public String bar;
        public String baz;

        public int getId() {
            return id;
        }

        public String getFoo() {
            return foo;
        }

        public String getBar() {
            return bar;
        }

        public String getBaz() {
            return baz;
        }
    }
}

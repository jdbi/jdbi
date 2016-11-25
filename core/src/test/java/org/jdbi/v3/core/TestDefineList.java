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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Arrays;
import java.util.List;

import org.jdbi.v3.core.mapper.FieldMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestDefineList {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private Handle handle;

    private List<Thing> list;

    @Before
    public void setUp() throws Exception {
        handle = db.getSharedHandle();
        handle.execute("create table thing (id identity primary key, foo varchar(50), bar varchar(50), baz varchar(50))");
        handle.execute("insert into thing (id, foo, bar, baz) values (?, ?, ?, ?)", 1, "foo1", "bar1", "baz1");
        handle.execute("insert into thing (id, foo, bar, baz) values (?, ?, ?, ?)", 2, "foo2", "bar2", "baz2");
        handle.registerRowMapper(FieldMapper.of(Thing.class));
    }

    @Test
    public void testDefineListSelect() throws Exception {
        list = handle.createQuery("select <columns> from thing order by id")
                .defineList("columns", Arrays.asList("id", "foo", "bar"))
                .mapTo(Thing.class)
                .list();
        assertThat(list)
                .extracting(Thing::getId, Thing::getFoo, Thing::getBar, Thing::getBaz)
                .containsExactly(
                        tuple(1, "foo1", "bar1", null),
                        tuple(2, "foo2", "bar2", null));

        list = handle.createQuery("select <columns> from thing order by id")
                .defineList("columns", Arrays.asList("id", "baz"))
                .mapTo(Thing.class)
                .list();
        assertThat(list)
                .extracting(Thing::getId, Thing::getFoo, Thing::getBar, Thing::getBaz)
                .containsExactly(
                        tuple(1, null, null, "baz1"),
                        tuple(2, null, null, "baz2"));
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

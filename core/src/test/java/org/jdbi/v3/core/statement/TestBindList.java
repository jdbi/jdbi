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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestBindList {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    private Handle handle;

    @Before
    public void setUp() {
        handle = dbRule.getSharedHandle();
        handle.registerRowMapper(FieldMapper.factory(Thing.class));
        handle.execute("create table thing (id identity primary key, foo varchar(50), bar varchar(50), baz varchar(50))");
        handle.execute("insert into thing (id, foo, bar, baz) values (?, ?, ?, ?)", 1, "foo1", "bar1", "baz1");
        handle.execute("insert into thing (id, foo, bar, baz) values (?, ?, ?, ?)", 2, "foo2", "bar2", "baz2");
    }

    @Test
    public void testBindList() {
        handle.createUpdate("insert into thing (<columns>) values (<values>)")
                .defineList("columns", "id", "foo")
                .bindList("values", 3, "abc")
                .execute();

        List<Thing> list = handle.createQuery("select id, foo from thing where id in (<ids>)")
                .bindList("ids", 1, 3)
                .mapTo(Thing.class)
                .list();
        assertThat(list)
                .extracting(Thing::getId, Thing::getFoo, Thing::getBar, Thing::getBaz)
                .containsExactly(
                        tuple(1, "foo1", null, null),
                        tuple(3, "abc", null, null));
    }

    @Test
    public void testBindListWithHashPrefixParser() {
        Jdbi jdbi = Jdbi.create(dbRule.getConnectionFactory());
        jdbi.setSqlParser(new HashPrefixSqlParser());
        jdbi.useHandle(handle -> {
            handle.registerRowMapper(FieldMapper.factory(Thing.class));
            handle.createUpdate("insert into thing (<columns>) values (<values>)")
                  .defineList("columns", "id", "foo")
                  .bindList("values", 3, "abc")
                  .execute();

            List<Thing> list = handle.createQuery("select id, foo from thing where id in (<ids>)")
                                     .bindList("ids", 1, 3)
                                     .mapTo(Thing.class)
                                     .list();
            assertThat(list)
                    .extracting(Thing::getId, Thing::getFoo, Thing::getBar, Thing::getBaz)
                    .containsExactly(
                            tuple(1, "foo1", null, null),
                            tuple(3, "abc", null, null));
        });
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

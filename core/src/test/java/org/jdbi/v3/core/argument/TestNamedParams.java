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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;
import org.junit.Rule;
import org.junit.Test;

public class TestNamedParams {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    @Test
    public void testInsert() throws Exception {
        Handle h = dbRule.openHandle();
        Update insert = h.createUpdate("insert into something (id, name) values (:id, :name)");
        insert.bind("id", 1);
        insert.bind("name", "Brian");
        int count = insert.execute();
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testDemo() throws Exception {
        Handle h = dbRule.getSharedHandle();
        h.createUpdate("insert into something (id, name) values (:id, :name)")
                .bind("id", 1)
                .bind("name", "Brian")
                .execute();
        h.execute("insert into something (id, name) values (?, ?)", 2, "Eric");
        h.execute("insert into something (id, name) values (?, ?)", 3, "Erin");

        List<Something> r = h.createQuery("select id, name from something " +
                                          "where name like :name " +
                                          "order by id")
                .bind("name", "Eri%")
                .mapToBean(Something.class)
                .list();

        assertThat(r).extracting(Something::getId).containsExactly(2, 3);
    }

    @Test
    public void testBeanPropertyBinding() throws Exception {
        Handle h = dbRule.openHandle();
        Something original = new Something(0, "Keith");

        assertThat(h
            .createUpdate("insert into something (id, name) values (:id, :name)")
            .bindBean(original)
            .execute())
            .isEqualTo(1);

        assertThat(h
            .select("select * from something where id = ?", original.getId())
            .mapToBean(Something.class)
            .findOnly())
            .isEqualTo(original);
    }

    @Test
    public void testBeanPropertyPrefixBinding() throws Exception {
        Handle h = dbRule.openHandle();
        Something original = new Something(0, "Keith");

        assertThat(h
            .createUpdate("insert into something (id, name) values (:my.id, :my.name)")
            .bindBean("my", original)
            .execute())
            .isEqualTo(1);

        assertThat(h
            .select("select * from something where id = ?", original.getId())
            .mapToBean(Something.class)
            .findOnly())
            .isEqualTo(original);
    }

    @Test
    public void testBeanPropertyNestedBinding() throws Exception {
        Handle h = dbRule.openHandle();

        Something thing = new Something(0, "Keith");

        assertThat(h
            .createUpdate("insert into something (id, name) values (:my.nested.id, :my.nested.name)")
            .bindBean("my", new Object() {
                @SuppressWarnings("unused")
                public Something getNested() {
                    return thing;
                }
            })
            .execute()).isEqualTo(1);

        assertThat(h
            .select("select * from something where id = ?", thing.getId())
            .mapToBean(Something.class)
            .findOnly())
            .isEqualTo(thing);
    }

    @Test
    public void testFieldsBinding() throws Exception {
        Handle h = dbRule.openHandle();

        assertThat(h
            .createUpdate("insert into something (id, name) values (:id, :name)")
            .bindFields(new PublicFields(0, "Keith"))
            .execute())
            .isEqualTo(1);

        assertThat(h
            .select("select * from something where id = ?", 0)
            .mapToBean(Something.class)
            .findOnly())
            .isEqualTo(new Something(0, "Keith"));
    }

    @Test
    public void testFieldsPrefixBinding() throws Exception {
        Handle h = dbRule.openHandle();

        assertThat(h
            .createUpdate("insert into something (id, name) values (:my.id, :my.name)")
            .bindFields("my", new PublicFields(0, "Keith"))
            .execute())
            .isEqualTo(1);

        assertThat(h
            .select("select * from something where id = ?", 0)
            .mapToBean(Something.class)
            .findOnly())
            .isEqualTo(new Something(0, "Keith"));
    }

    @Test
    public void testFieldsNestedBinding() throws Exception {
        Handle h = dbRule.openHandle();

        assertThat(h
            .createUpdate("insert into something (id, name) values (:my.nested.id, :my.nested.name)")
            .bindFields("my", new Object() {
                @SuppressWarnings("unused")
                public PublicFields nested = new PublicFields(0, "Keith");
            })
            .execute())
            .isEqualTo(1);

        assertThat(h
            .select("select * from something where id = ?", 0)
            .mapToBean(Something.class)
            .findOnly())
            .isEqualTo(new Something(0, "Keith"));
    }

    public static class PublicFields {
        public int id = 0;
        public String name = "Keith";

        public PublicFields(int id, String name) {

            this.id = id;
            this.name = name;
        }
    }

    @Test
    public void testFunctionsBinding() throws Exception {
        Handle h = dbRule.openHandle();

        assertThat(h
            .createUpdate("insert into something (id, name) values (:id, :name)")
            .bindMethods(new NoArgFunctions(0, "Keith"))
            .execute())
            .isEqualTo(1);

        assertThat(h
            .select("select * from something where id = ?", 0)
            .mapToBean(Something.class)
            .findOnly())
            .isEqualTo(new Something(0, "Keith"));
    }

    @Test
    public void testFunctionsPrefixBinding() throws Exception {
        Handle h = dbRule.openHandle();

        assertThat(h
            .createUpdate("insert into something (id, name) values (:my.id, :my.name)")
            .bindMethods("my", new NoArgFunctions(0, "Keith"))
            .execute())
            .isEqualTo(1);

        assertThat(h
            .select("select * from something where id = ?", 0)
            .mapToBean(Something.class)
            .findOnly())
            .isEqualTo(new Something(0, "Keith"));
    }

    @Test
    public void testFunctionsNestedBinding() throws Exception {
        Handle h = dbRule.openHandle();

        assertThat(h
            .createUpdate("insert into something (id, name) values (:my.nested.id, :my.nested.name)")
            .bindMethods("my", new Object() {
                @SuppressWarnings("unused")
                public NoArgFunctions nested() {
                    return new NoArgFunctions(0, "Keith");
                }
            })
            .execute())
            .isEqualTo(1);

        assertThat(h
            .select("select * from something where id = ?", 0)
            .mapToBean(Something.class)
            .findOnly())
            .isEqualTo(new Something(0, "Keith"));
    }

    public static class NoArgFunctions {
        private final int i;
        private final String s;

        public NoArgFunctions(int i, String s) {
            this.i = i;
            this.s = s;
        }

        public int id() {
            return i;
        }

        public String name() {
            return s;
        }
    }
    @Test
    public void testMapKeyBinding() throws Exception {
        Handle h = dbRule.openHandle();
        Update s = h.createUpdate("insert into something (id, name) values (:id, :name)");
        Map<String, Object> args = new HashMap<>();
        args.put("id", 0);
        args.put("name", "Keith");
        s.bindMap(args);
        int insert_count = s.execute();

        Query q = h.createQuery("select * from something where id = :id").bind("id", 0);
        final Something fromDb = q.mapToBean(Something.class).findOnly();

        assertThat(insert_count).isEqualTo(1);
        assertThat(fromDb).extracting(Something::getId, Something::getName).containsExactly(0, "Keith");
    }

    @Test
    public void testCascadedLazyArgs() throws Exception {
        Handle h = dbRule.openHandle();
        Update s = h.createUpdate("insert into something (id, name) values (:id, :name)");
        Map<String, Object> args = new HashMap<>();
        args.put("id", 0);
        s.bindMap(args);
        s.bindBean(new Object() {
            @SuppressWarnings("unused")
            public String getName() { return "Keith"; }
        });
        int insert_count = s.execute();
        assertThat(insert_count).isEqualTo(1);
        Something something = h.createQuery("select id, name from something").mapToBean(Something.class).findOnly();
        assertThat(something).isEqualTo(new Something(0, "Keith"));
    }
}

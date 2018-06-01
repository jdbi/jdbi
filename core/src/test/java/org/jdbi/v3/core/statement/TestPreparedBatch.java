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

import com.google.common.collect.ImmutableMap;
import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;

public class TestPreparedBatch {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    private Handle h;

    @Before
    public void openHandle() {
        h = dbRule.openHandle();
    }

    @After
    public void closeHandle() {
        h.close();
    }

    @Test
    public void testBindBatch() throws Exception {
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b.bind("id", 1).bind("name", "Eric").add();
        b.bind("id", 2).bind("name", "Brian").add();
        b.bind("id", 3).bind("name", "Keith").add();
        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).hasSize(3);
        assertThat(r.get(2).getName()).isEqualTo("Keith");
    }

    @Test
    public void testBigishBatch() throws Exception {
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        int count = 100;
        for (int i = 0; i < count; ++i) {
            b.bind("id", i).bind("name", "A Name").add();

        }
        b.execute();

        int row_count = h.createQuery("select count(id) from something").mapTo(int.class).findOnly();

        assertThat(row_count).isEqualTo(count);
    }

    @Test
    public void testBindProperties() throws Exception {
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (?, ?)");

        b.add(0, "Keith");
        b.add(1, "Eric");
        b.add(2, "Brian");

        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).hasSize(3);
        assertThat(r.get(2).getName()).isEqualTo("Brian");
    }

    @Test
    public void testBindMaps() throws Exception {
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b.add(ImmutableMap.of("id", 0, "name", "Keith"));
        b.add(ImmutableMap.of("id", 1, "name", "Eric"));
        b.add(ImmutableMap.of("id", 2, "name", "Brian"));

        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).hasSize(3);
        assertThat(r.get(2).getName()).isEqualTo("Brian");
    }

    @Test
    public void testMixedModeBatch() throws Exception {
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        Map<String, Object> one = ImmutableMap.of("id", 0);
        b.bind("name", "Keith").add(one);
        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).extracting(Something::getName).containsExactly("Keith");
    }

    @Test
    public void testPositionalBinding() throws Exception {
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (?, ?)");

        b.bind(0, 0).bind(1, "Keith").add().execute();

        List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).extracting(Something::getName).containsExactly("Keith");
    }

    @Test
    public void testForgotFinalAdd() throws Exception {
        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b.bind("id", 1);
        b.bind("name", "Jeff");
        b.add();

        b.bind("id", 2);
        b.bind("name", "Tom");
        // forgot to add() here but we fix it up

        b.execute();

        assertThat(h.createQuery("select name from something order by id").mapTo(String.class).list())
                .containsExactly("Jeff", "Tom");
    }

    @Test
    public void testContextGetsBinding() throws Exception {
        try {
            h.prepareBatch("insert into something (id, name) values (:id, :name)")
                .bind("id", 0)
                .bind("name", "alice")
                .add()
                .bind("id", 0)
                .bind("name", "bob")
                .add()
                .execute();
            fail("expected exception");
        } catch (UnableToExecuteStatementException e) {
            final StatementContext ctx = e.getStatementContext();
            assertThat(ctx.getBinding().findForName("name", ctx).toString()).contains("bob");
        }
    }

    @Test
    public void testMultipleExecuteBindBean() {
        final PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b.bindBean(new Something(1, "Eric")).add();
        b.bindBean(new Something(2, "Brian")).add();
        b.execute();

        // bindings should be cleared after execute()

        b.bindBean(new Something(3, "Keith")).add();
        b.execute();

        final List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).extracting(Something::getId, Something::getName)
                .containsExactly(tuple(1, "Eric"), tuple(2, "Brian"), tuple(3, "Keith"));
    }

    @Test
    public void testMultipleExecuteBind() {
        final PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b.bind("id", 1).bind("name", "Eric").add();
        b.bind("id", 2).bind("name", "Brian").add();
        b.execute();

        // bindings should be cleared after execute()

        b.bind("id", 3).bind("name", "Keith").add();
        b.execute();

        final List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).extracting(Something::getId, Something::getName)
                .containsExactly(tuple(1, "Eric"), tuple(2, "Brian"), tuple(3, "Keith"));
    }

    @Test
    public void testMultipleExecuteBindFields() {
        h.registerRowMapper(ConstructorMapper.factory(PublicSomething.class));
        final PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b.bindFields(new PublicSomething(1, "Eric")).add();
        b.bindFields(new PublicSomething(2, "Brian")).add();
        b.execute();

        // bindings should be cleared after execute()

        b.bindFields(new PublicSomething(3, "Keith")).add();
        b.execute();

        final List<PublicSomething> r = h.createQuery("select * from something order by id").mapTo(PublicSomething.class).list();
        assertThat(r).extracting(s -> s.id, s -> s.name).containsExactly(tuple(1, "Eric"), tuple(2, "Brian"), tuple(3, "Keith"));
    }

    public static class PublicSomething {
        public int id;
        public String name;

        @ConstructorProperties({"id", "name"})
        public PublicSomething(Integer id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}

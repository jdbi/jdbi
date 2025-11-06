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

import java.beans.ConstructorProperties;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;

public class TestPreparedBatch {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER);

    @Test
    public void emptyBatch() {
        Handle h = h2Extension.getSharedHandle();

        final PreparedBatch batch = h.prepareBatch("insert into something (id, name) values (:id, :name)");
        assertThat(batch.execute()).isEmpty();
        assertThat(batch.getContext().isClean()).isTrue();
    }

    @Test
    public void testBindBatch() {
        Handle h = h2Extension.getSharedHandle();

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
    public void testBigishBatch() {
        Handle h = h2Extension.getSharedHandle();

        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        int count = 100;
        for (int i = 0; i < count; ++i) {
            b.bind("id", i).bind("name", "A Name").add();

        }
        b.execute();

        int rowCount = h.createQuery("select count(id) from something").mapTo(int.class).one();

        assertThat(rowCount).isEqualTo(count);
    }

    @Test
    public void testBindProperties() {
        Handle h = h2Extension.getSharedHandle();

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
    public void testBindMaps() {
        Handle h = h2Extension.getSharedHandle();

        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b.add(ImmutableMap.of("id", 0, "name", "Keith"));
        b.add(ImmutableMap.of("id", 1, "name", "Eric"));
        b.add(ImmutableMap.of("id", 2, "name", "Brian"));

        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r)
            .extracting(Something::getName)
            .containsExactly("Keith", "Eric", "Brian");
    }

    @Test
    public void testMixedModeBatch() {
        Handle h = h2Extension.getSharedHandle();

        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        Map<String, Object> one = ImmutableMap.of("id", 0);
        b.bind("name", "Keith").add(one);
        b.execute();

        List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).extracting(Something::getName).containsExactly("Keith");
    }

    @Test
    public void testPositionalBinding() {
        Handle h = h2Extension.getSharedHandle();

        PreparedBatch b = h.prepareBatch("insert into something (id, name) values (?, ?)");

        b.bind(0, 0).bind(1, "Keith").add().execute();

        List<Something> r = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(r).extracting(Something::getName).containsExactly("Keith");
    }

    @Test
    public void testForgotFinalAdd() {
        Handle h = h2Extension.getSharedHandle();

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
    public void testContextGetsBinding() {
        Handle h = h2Extension.getSharedHandle();

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
        Handle h = h2Extension.getSharedHandle();

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
        Handle h = h2Extension.getSharedHandle();

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
        Handle h = h2Extension.getSharedHandle();

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

    @Test
    public void testNestedNotPrepareable() {
        Handle h = h2Extension.getSharedHandle();

        h.registerArgument(new WrappedIntArgumentFactory());
        h.registerRowMapper(ConstructorMapper.factory(WrappedIntPublicSomething.class));
        h.registerColumnMapper(new WrappedIntColumnMapperFactory());
        final PreparedBatch b = h.prepareBatch("insert into something (id, name) values (:id, :name)");

        b.bindFields(new WrappedIntPublicSomething(new WrappedInt(2), "Sally")).add();
        b.bindFields(new WrappedIntPublicSomething(new WrappedInt(3), "Erica")).add();
        b.execute();

        final List<WrappedIntPublicSomething> r = h.createQuery("select * from something order by id").mapTo(WrappedIntPublicSomething.class).list();
        assertThat(r).extracting(s -> s.id, s -> s.name).containsExactly(tuple(new WrappedInt(2), "Sally"), tuple(new WrappedInt(3), "Erica"));
    }

    @Test
    public void testBindNull() {
        Handle handle = h2Extension.getSharedHandle();
        handle.execute("CREATE TABLE record (b bool)");
        assertThat(handle.prepareBatch("INSERT INTO record (b) VALUES (:bVal)")
            .bind("bVal", false).add()
            .bindNull("bVal", Types.BOOLEAN).add()
            .bindByType("bVal", null, boolean.class).add()
            .execute()).containsExactly(1, 1, 1);
    }

    @Test
    public void testDefaultBatchChunkSize() {
        Handle h = h2Extension.getSharedHandle();
        PreparedBatch batch = h.prepareBatch("insert into something (id, name) values (:id, :name)");
        assertThat(batch.getBatchChunkSize()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void testSetBatchChunkSize() {
        Handle h = h2Extension.getSharedHandle();
        PreparedBatch batch = h.prepareBatch("insert into something (id, name) values (:id, :name)");
        batch.setBatchChunkSize(100);
        assertThat(batch.getBatchChunkSize()).isEqualTo(100);
        batch.setBatchChunkSize(5000);
        assertThat(batch.getBatchChunkSize()).isEqualTo(5000);
    }

    @Test
    public void testInvalidBatchChunkSize() {
        Handle h = h2Extension.getSharedHandle();
        PreparedBatch batch = h.prepareBatch("insert into something (id, name) values (:id, :name)");
        try {
            batch.setBatchChunkSize(0);
            fail("Expected IllegalArgumentException for chunk size 0");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("Batch chunk size must be greater than zero");
        }
        try {
            batch.setBatchChunkSize(-1);
            fail("Expected IllegalArgumentException for negative chunk size");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("Batch chunk size must be greater than zero");
        }
    }

    @Test
    public void testBatchChunkSizeExecution() {
        Handle h = h2Extension.getSharedHandle();
        PreparedBatch batch = h.prepareBatch("insert into something (id, name) values (:id, :name)");
        // Set chunk size to 3
        batch.setBatchChunkSize(3);
        // Add 7 items (will require 3 chunks: 3, 3, 1)
        for (int i = 0; i < 7; i++) {
            batch.bind("id", i).bind("name", "Name" + i).add();
        }
        int[] results = batch.execute();
        // Should get 7 update counts (one for each row)
        assertThat(results).hasSize(7);
        // All should be successful inserts
        assertThat(results).containsOnly(1);
        // Verify all data was inserted
        List<Something> records = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(records).hasSize(7);
        assertThat(records).extracting(Something::getName)
            .containsExactly("Name0", "Name1", "Name2", "Name3", "Name4", "Name5", "Name6");
    }

    @Test
    public void testBatchChunkSizeWithSingleChunk() {
        Handle h = h2Extension.getSharedHandle();
        PreparedBatch batch = h.prepareBatch("insert into something (id, name) values (:id, :name)");
        // Set chunk size larger than number of items
        batch.setBatchChunkSize(10);
        // Add only 3 items
        for (int i = 0; i < 3; i++) {
            batch.bind("id", i).bind("name", "SingleChunk" + i).add();
        }
        int[] results = batch.execute();
        assertThat(results).hasSize(3);
        assertThat(results).containsOnly(1);
        // Verify all data was inserted
        List<Something> records = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(records).hasSize(3);
        assertThat(records).extracting(Something::getName)
            .containsExactly("SingleChunk0", "SingleChunk1", "SingleChunk2");
    }

    @Test
    public void testBatchChunkSizeWithLargeBatch() {
        Handle h = h2Extension.getSharedHandle();
        PreparedBatch batch = h.prepareBatch("insert into something (id, name) values (:id, :name)");
        // Set small chunk size
        batch.setBatchChunkSize(5);
        int totalItems = 23; // Will require 5 chunks: 5, 5, 5, 5, 3
        for (int i = 0; i < totalItems; i++) {
            batch.bind("id", i).bind("name", "Large" + i).add();
        }
        int[] results = batch.execute();
        assertThat(results).hasSize(totalItems);
        assertThat(results).containsOnly(1);
        // Verify all data was inserted
        int recordCount = h.createQuery("select count(*) from something").mapTo(int.class).one();
        assertThat(recordCount).isEqualTo(totalItems);
    }

    @Test
    public void testBatchChunkSizeFluentInterface() {
        Handle h = h2Extension.getSharedHandle();
        // Test that setBatchChunkSize returns this for method chaining
        PreparedBatch batch = h.prepareBatch("insert into something (id, name) values (:id, :name)")
            .setBatchChunkSize(2)
            .bind("id", 1).bind("name", "Fluent1").add()
            .bind("id", 2).bind("name", "Fluent2").add()
            .bind("id", 3).bind("name", "Fluent3").add();
        assertThat(batch.getBatchChunkSize()).isEqualTo(2);
        int[] results = batch.execute();
        assertThat(results).hasSize(3);
        assertThat(results).containsOnly(1);
        // Verify data
        List<Something> records = h.createQuery("select * from something order by id").mapToBean(Something.class).list();
        assertThat(records).hasSize(3);
        assertThat(records).extracting(Something::getName)
            .containsExactly("Fluent1", "Fluent2", "Fluent3");
    }

    @Test
    public void testBatchChunkSizeWithEmptyBatch() {
        Handle h = h2Extension.getSharedHandle();
        PreparedBatch batch = h.prepareBatch("insert into something (id, name) values (:id, :name)");
        batch.setBatchChunkSize(5);
        int[] results = batch.execute();
        assertThat(results).isEmpty();
        assertThat(batch.getBatchChunkSize()).isEqualTo(5);
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

    public static class WrappedIntPublicSomething {
        public WrappedInt id;
        public String name;

        @ConstructorProperties({"id", "name"})
        public WrappedIntPublicSomething(WrappedInt id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class WrappedInt {
        final int i;

        public WrappedInt(final int i) {
            this.i = i;
        }

        @Override
        public int hashCode() {
            return i;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            WrappedInt other = (WrappedInt) obj;
            return i == other.i;
        }
    }

    public static class WrappedIntArgumentFactory implements ArgumentFactory {
        @Override
        public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
            return type == WrappedInt.class
                    ? Optional.of((p, s, c) -> s.setInt(p, ((WrappedInt) value).i))
                    : Optional.empty();
        }
    }

    public static class WrappedIntColumnMapperFactory implements ColumnMapper<WrappedInt> {
        @Override
        public WrappedInt map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return new WrappedInt(r.getInt(columnNumber));
        }
    }
}

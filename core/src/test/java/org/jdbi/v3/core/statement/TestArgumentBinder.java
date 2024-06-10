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

import java.sql.Types;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.PgDatabaseExtension;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestArgumentBinder {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public PgDatabaseExtension pgDatabaseExtension = PgDatabaseExtension.instance(pg)
        .withInitializer(handle ->
            handle.execute("CREATE TABLE binder_test (i INT, u UUID, s VARCHAR, t timestamp with time zone default current_timestamp)"));

    @Test
    public void testIntBinding() {
        try (Handle h = pgDatabaseExtension.openHandle()) {
            int count = h.createUpdate("INSERT INTO binder_test (i) values (:i)")
                .bind("i", 100)
                .execute();

            assertThat(count).isOne();

            int total = h.createQuery("SELECT COUNT(1) from binder_test WHERE i = :i")
                .bind("i", 100)
                .mapTo(Integer.class)
                .one();

            assertThat(total).isOne();
        }
    }

    @Test
    public void testIntBatchBinding() {
        try (Handle h = pgDatabaseExtension.openHandle()) {
            PreparedBatch b = h.prepareBatch("INSERT INTO binder_test (i) values (:i)");

            b.bind("i", 100)
                .add();

            int[] count = b.execute();
            assertThat(count).hasSize(1);
            assertThat(count[0]).isOne();

            int total = h.createQuery("SELECT COUNT(1) from binder_test WHERE i = :i")
                .bind("i", 100)
                .mapTo(Integer.class)
                .one();

            assertThat(total).isOne();
        }
    }

    @Test
    public void testIntArgument() {
        try (Handle h = pgDatabaseExtension.openHandle()) {
            int count = h.createUpdate("INSERT INTO binder_test (i) values (:i)")
                .bind("i", (position, statement, ctx) -> statement.setInt(position, 200))
                .execute();

            assertThat(count).isOne();

            int total = h.createQuery("SELECT COUNT(1) from binder_test WHERE i = :i")
                .bind("i", (position, statement, ctx) -> statement.setInt(position, 200))
                .mapTo(Integer.class)
                .one();

            assertThat(total).isOne();
        }
    }

    @Test
    public void testIntBatchArgument() {
        try (Handle h = pgDatabaseExtension.openHandle()) {
            PreparedBatch b = h.prepareBatch("INSERT INTO binder_test (i) values (:i)");

            b.bind("i", (position, statement, ctx) -> statement.setInt(position, 200))
                .add();

            int[] count = b.execute();
            assertThat(count).hasSize(1);
            assertThat(count[0]).isOne();

            int total = h.createQuery("SELECT COUNT(1) from binder_test WHERE i = :i")
                .bind("i", (position, statement, ctx) -> statement.setInt(position, 200))
                .mapTo(Integer.class)
                .one();

            assertThat(total).isOne();
        }
    }

    @Test
    public void testBean() {
        TestBean testBean = new TestBean(300, "hello, world");
        try (Handle h = pgDatabaseExtension.openHandle()) {
            int count = h.createUpdate("INSERT INTO binder_test (i, s) values (:i, :s)")
                .bindBean(testBean)
                .execute();

            assertThat(count).isOne();

            TestBean result = h.createQuery("SELECT * from binder_test WHERE i = :i")
                .bind("i", 300)
                .map(ConstructorMapper.of(TestBean.class))
                .one();

            assertThat(result.getI()).isEqualTo(300);
            assertThat(result.getS()).isEqualTo("hello, world");
        }
    }

    @Test
    public void testBatchBean() {
        TestBean testBean = new TestBean(300, "hello, world");
        try (Handle h = pgDatabaseExtension.openHandle()) {
            PreparedBatch b = h.prepareBatch("INSERT INTO binder_test (i, s) values (:i, :s)");

            b.bindBean(testBean)
                .add();

            int[] count = b.execute();
            assertThat(count).hasSize(1);
            assertThat(count[0]).isOne();

            TestBean result = h.createQuery("SELECT * from binder_test WHERE i = :i")
                .bind("i", 300)
                .map(ConstructorMapper.of(TestBean.class))
                .one();

            assertThat(result.getI()).isEqualTo(300);
            assertThat(result.getS()).isEqualTo("hello, world");
        }
    }

    @Test
    public void testNullBean() {
        TestBean testBean = new TestBean(400, null);
        try (Handle h = pgDatabaseExtension.openHandle()) {
            int count = h.createUpdate("INSERT INTO binder_test (i, s) values (:i, :s)")
                .bindBean(testBean)
                .execute();

            assertThat(count).isOne();

            TestBean result = h.createQuery("SELECT * from binder_test WHERE i = :i")
                .bind("i", 400)
                .map(ConstructorMapper.of(TestBean.class))
                .one();

            assertThat(result.getI()).isEqualTo(400);
            assertThat(result.getS()).isNull();
        }
    }

    @Test
    public void testNullBatchBean() {
        TestBean testBean = new TestBean(400, null);
        try (Handle h = pgDatabaseExtension.openHandle()) {
            PreparedBatch b = h.prepareBatch("INSERT INTO binder_test (i, s) values (:i, :s)");

            b.bindBean(testBean)
                .add();

            int[] count = b.execute();
            assertThat(count).hasSize(1);
            assertThat(count[0]).isOne();

            TestBean result = h.createQuery("SELECT * from binder_test WHERE i = :i")
                .bind("i", 400)
                .map(ConstructorMapper.of(TestBean.class))
                .one();

            assertThat(result.getI()).isEqualTo(400);
            assertThat(result.getS()).isNull();
        }
    }

    @Test
    public void testNullBinding() {
        Object stringValue = null;
        try (Handle h = pgDatabaseExtension.openHandle()) {
            int count = h.createUpdate("INSERT INTO binder_test (i, s) values (:i, :s)")
                .bind("i", 100)
                .bind("s", stringValue)
                .execute();

            assertThat(count).isOne();

            String value = h.createQuery("SELECT s from binder_test WHERE i = :i")
                .bind("i", 100)
                .mapTo(String.class)
                .one();

            assertThat(value).isNull();
        }
    }

    @Test
    public void testNullBatchBinding() {
        Object stringValue = null;
        try (Handle h = pgDatabaseExtension.openHandle()) {
            PreparedBatch b = h.prepareBatch("INSERT INTO binder_test (i, s) values (:i, :s)");

            b.bind("i", 100)
                .bind("s", stringValue)
                .add();

            int[] count = b.execute();
            assertThat(count).hasSize(1);
            assertThat(count[0]).isOne();

            String value = h.createQuery("SELECT s from binder_test WHERE i = :i")
                .bind("i", 100)
                .mapTo(String.class)
                .one();

            assertThat(value).isNull();
        }
    }

    @Test
    public void testNullArgument() {
        try (Handle h = pgDatabaseExtension.openHandle()) {
            int count = h.createUpdate("INSERT INTO binder_test (i, s) values (:i, :s)")
                .bind("i", 100)
                .bindNull("s", Types.VARCHAR)
                .execute();

            assertThat(count).isOne();

            String value = h.createQuery("SELECT s from binder_test WHERE i = :i")
                .bind("i", 100)
                .mapTo(String.class)
                .one();

            assertThat(value).isNull();
        }
    }

    @Test
    public void testNullBatchArgument() {
        try (Handle h = pgDatabaseExtension.openHandle()) {
            PreparedBatch b = h.prepareBatch("INSERT INTO binder_test (i, s) values (:i, :s)");

            b.bind("i", 100)
                .bindNull("s", Types.VARCHAR)
                .add();

            int[] count = b.execute();
            assertThat(count).hasSize(1);
            assertThat(count[0]).isOne();

            String value = h.createQuery("SELECT s from binder_test WHERE i = :i")
                .bind("i", 100)
                .mapTo(String.class)
                .one();

            assertThat(value).isNull();
        }
    }

    @Test
    public void testMissingParamThrowsError() {
        try (Handle h = pgDatabaseExtension.openHandle()) {
            assertThatThrownBy(
                // Right number of params, but indexes are non-contiguous by mistake.
                h.createQuery("SELECT COUNT(1) from binder_test WHERE i = ? OR i = ? OR i = ?")
                    .bind(0, 100)
                    .bind(1, 101)
                    .bind(3, 102)
                    .mapTo(Integer.class)
                    ::one
            ).isInstanceOf(UnableToCreateStatementException.class)
                .hasMessageStartingWith("Missing positional parameter 2 in binding:{positional:{0:100,1:101,3:102}, named:{}, finder:[]}");
        }
    }

    @Test
    public void testOneBasedIndexingThrowsError() {
        try (Handle h = pgDatabaseExtension.openHandle()) {
            assertThatThrownBy(
                // Mistakenly thought the params are 1-based.
                h.createQuery("SELECT COUNT(1) from binder_test WHERE i = ?")
                    .bind(1, 100)
                    .mapTo(Integer.class)
                    ::one
            ).isInstanceOf(UnableToCreateStatementException.class)
                .hasMessageStartingWith("Missing positional parameter 0 in binding:{positional:{1:100}, named:{}, finder:[]}");
        }
    }

    public static class TestBean {

        private final int i;
        private final String s;

        public TestBean(int i, String s) {
            this.i = i;
            this.s = s;
        }

        public int getI() {
            return i;
        }

        public String getS() {
            return s;
        }
    }

}

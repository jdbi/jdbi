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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

            assertEquals(1, count);

            int total = h.createQuery("SELECT COUNT(1) from binder_test WHERE i = :i")
                .bind("i", 100)
                .mapTo(Integer.class)
                .one();

            assertEquals(1, total);
        }
    }

    @Test
    public void testIntBatchBinding() {
        try (Handle h = pgDatabaseExtension.openHandle()) {
            PreparedBatch b = h.prepareBatch("INSERT INTO binder_test (i) values (:i)");

            b.bind("i", 100)
                .add();

            int[] count = b.execute();
            assertEquals(1, count.length);
            assertEquals(1, count[0]);

            int total = h.createQuery("SELECT COUNT(1) from binder_test WHERE i = :i")
                .bind("i", 100)
                .mapTo(Integer.class)
                .one();

            assertEquals(1, total);
        }
    }

    @Test
    public void testIntArgument() {
        try (Handle h = pgDatabaseExtension.openHandle()) {
            int count = h.createUpdate("INSERT INTO binder_test (i) values (:i)")
                .bind("i", (position, statement, ctx) -> statement.setInt(position, 200))
                .execute();

            assertEquals(1, count);

            int total = h.createQuery("SELECT COUNT(1) from binder_test WHERE i = :i")
                .bind("i", (position, statement, ctx) -> statement.setInt(position, 200))
                .mapTo(Integer.class)
                .one();

            assertEquals(1, total);
        }
    }

    @Test
    public void testIntBatchArgument() {
        try (Handle h = pgDatabaseExtension.openHandle()) {
            PreparedBatch b = h.prepareBatch("INSERT INTO binder_test (i) values (:i)");

            b.bind("i", (position, statement, ctx) -> statement.setInt(position, 200))
                .add();

            int[] count = b.execute();
            assertEquals(1, count.length);
            assertEquals(1, count[0]);

            int total = h.createQuery("SELECT COUNT(1) from binder_test WHERE i = :i")
                .bind("i", (position, statement, ctx) -> statement.setInt(position, 200))
                .mapTo(Integer.class)
                .one();

            assertEquals(1, total);
        }
    }

    @Test
    public void testBean() {
        TestBean testBean = new TestBean(300, "hello, world");
        try (Handle h = pgDatabaseExtension.openHandle()) {
            int count = h.createUpdate("INSERT INTO binder_test (i, s) values (:i, :s)")
                .bindBean(testBean)
                .execute();

            assertEquals(1, count);

            TestBean result = h.createQuery("SELECT * from binder_test WHERE i = :i")
                .bind("i", 300)
                .map(ConstructorMapper.of(TestBean.class))
                .one();

            assertEquals(300, result.getI());
            assertEquals("hello, world", result.getS());
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
            assertEquals(1, count.length);
            assertEquals(1, count[0]);

            TestBean result = h.createQuery("SELECT * from binder_test WHERE i = :i")
                .bind("i", 300)
                .map(ConstructorMapper.of(TestBean.class))
                .one();

            assertEquals(300, result.getI());
            assertEquals("hello, world", result.getS());
        }
    }

    @Test
    public void testNullBean() {
        TestBean testBean = new TestBean(400, null);
        try (Handle h = pgDatabaseExtension.openHandle()) {
            int count = h.createUpdate("INSERT INTO binder_test (i, s) values (:i, :s)")
                .bindBean(testBean)
                .execute();

            assertEquals(1, count);

            TestBean result = h.createQuery("SELECT * from binder_test WHERE i = :i")
                .bind("i", 400)
                .map(ConstructorMapper.of(TestBean.class))
                .one();

            assertEquals(400, result.getI());
            assertNull(result.getS());
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
            assertEquals(1, count.length);
            assertEquals(1, count[0]);

            TestBean result = h.createQuery("SELECT * from binder_test WHERE i = :i")
                .bind("i", 400)
                .map(ConstructorMapper.of(TestBean.class))
                .one();

            assertEquals(400, result.getI());
            assertNull(result.getS());
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

            assertEquals(1, count);

            String value = h.createQuery("SELECT s from binder_test WHERE i = :i")
                .bind("i", 100)
                .mapTo(String.class)
                .one();

            assertNull(value);
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
            assertEquals(1, count.length);
            assertEquals(1, count[0]);

            String value = h.createQuery("SELECT s from binder_test WHERE i = :i")
                .bind("i", 100)
                .mapTo(String.class)
                .one();

            assertNull(value);
        }
    }

    @Test
    public void testNullArgument() {
        try (Handle h = pgDatabaseExtension.openHandle()) {
            int count = h.createUpdate("INSERT INTO binder_test (i, s) values (:i, :s)")
                .bind("i", 100)
                .bindNull("s", Types.VARCHAR)
                .execute();

            assertEquals(1, count);

            String value = h.createQuery("SELECT s from binder_test WHERE i = :i")
                .bind("i", 100)
                .mapTo(String.class)
                .one();

            assertNull(value);
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
            assertEquals(1, count.length);
            assertEquals(1, count[0]);

            String value = h.createQuery("SELECT s from binder_test WHERE i = :i")
                .bind("i", 100)
                .mapTo(String.class)
                .one();

            assertNull(value);
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

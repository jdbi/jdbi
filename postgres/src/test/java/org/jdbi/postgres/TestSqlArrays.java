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
package org.jdbi.postgres;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.core.Handle;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.sqlobject.SingleValue;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.config.RegisterRowMapper;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.statement.SqlBatch;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSqlArrays {

    private static final String U_SELECT = "SELECT u FROM uuids";
    private static final String U_INSERT = "INSERT INTO uuids VALUES(:uuids, NULL, NULL)";
    private static final String I_SELECT = "SELECT i FROM uuids";
    private static final String I_INSERT = "INSERT INTO uuids VALUES(NULL, :ints, NULL)";
    private static final String T_SELECT = "SELECT t FROM uuids";
    private static final String T_INSERT = "INSERT INTO uuids VALUES(NULL, NULL, :instants)";

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withPlugins(new SqlObjectPlugin(), new PostgresPlugin())
        .withInitializer((ds, h) -> h.useTransaction(th -> {
            th.execute("DROP TABLE IF EXISTS uuids");
            th.execute("CREATE TABLE uuids (u UUID[], i INT[], t TIMESTAMPTZ[])");
        }));

    private Handle handle;
    private ArrayObject ao;

    @BeforeEach
    public void setUp() {
        handle = pgExtension.openHandle()
            // register array type to the handle
            .registerArrayType(Instant.class, "timestamptz");

        // attach the array object to the handle as well.
        ao = handle.attach(ArrayObject.class);
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    private final UUID[] testUuids = new UUID[] {
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
    };

    private final int[] testInts = new int[] {
        5, 4, -6, 1, 9, Integer.MAX_VALUE, Integer.MIN_VALUE
    };

    private final Instant[] testInstants = new Instant[] {
        Instant.EPOCH,
        Instant.now().truncatedTo(ChronoUnit.MILLIS),
        Instant.ofEpochSecond(Integer.MIN_VALUE),
        Instant.ofEpochSecond(Integer.MAX_VALUE),
        // amazingly, according to ISO 8601, everything before 1583 is a mess...
        Instant.parse("1583-01-01T00:00:00Z"),
        // and anything after 9999 is as well...
        Instant.parse("9999-12-31T23:59:59Z"),
    };

    @Test
    public void testUuidArray() {
        ao.insertUuidArray(testUuids);
        assertThat(ao.fetchUuidArray()).containsExactly(testUuids);
    }

    @Test
    public void testUuidList() {
        ao.insertUuidList(Arrays.asList(testUuids));
        assertThat(ao.fetchUuidList()).contains(testUuids);
    }

    @Test
    public void testUuidArrayList() {
        ao.insertUuidList(Arrays.asList(testUuids));
        assertThat(ao.fetchUuidArrayList()).contains(testUuids);
    }

    @Test
    public void testUuidLinkedList() {
        ao.insertUuidList(Arrays.asList(testUuids));
        assertThat(ao.fetchUuidLinkedList()).contains(testUuids);
    }

    @Test
    public void testUuidCopyOnWriteArrayList() {
        ao.insertUuidList(Arrays.asList(testUuids));
        assertThat(ao.fetchUuidCopyOnWriteArrayList()).contains(testUuids);
    }

    @Test
    public void testIntArray() {
        ao.insertIntArray(testInts);
        int[] actuals = ao.fetchIntArray();
        assertThat(actuals).containsExactly(testInts);
    }

    @Test
    public void testEmptyIntArray() {
        ao.insertIntArray(new int[0]);
        assertThat(ao.fetchIntArray()).isEmpty();
    }

    @Test
    public void testBoxedIntArray() {
        Integer[] source = IntStream.of(testInts).mapToObj(Integer::valueOf).toArray(Integer[]::new);
        ao.insertBoxedIntArray(source);
        Integer[] actuals = ao.fetchBoxedIntArray();
        assertThat(actuals).containsExactly(source);
    }

    @Test
    public void testObjectArray() {
        ao.insertIntArray(testInts);
        Object[] actuals = ao.fetchObjectArray();
        Object[] expecteds = IntStream.of(testInts).mapToObj(Integer::valueOf).toArray(Object[]::new);
        assertThat(actuals).containsExactly(expecteds);
    }

    @Test
    public void testIntList() {
        List<Integer> testIntList = Arrays.stream(testInts).boxed().collect(Collectors.toList());
        ao.insertIntList(testIntList);
        assertThat(ao.fetchIntList()).containsExactlyElementsOf(testIntList);
    }

    @Test
    public void testNullArray() {
        ao.insertUuidArray(null);
        assertThat(ao.fetchUuidArray()).isNull();
    }

    @Test
    public void testNullList() {
        ao.insertUuidList(null);
        assertThat(ao.fetchUuidLinkedList()).isNull();
    }

    @Test
    public void testFloatArray() {
        final float[] expected = new float[] {1, 2, 3};
        assertThat(handle.createQuery("select :array")
                .bind("array", expected)
                .mapTo(float[].class)
                .one())
            .isEqualTo(expected);
    }

    @Test
    public void testDoubleArray() {
        final double[] expected = new double[] {1, 2, 3};
        assertThat(handle.createQuery("select :array")
                .bind("array", expected)
                .mapTo(double[].class)
                .one())
            .isEqualTo(expected);
    }

    @Test
    public void testReusedArrayWithString() throws Exception {
        handle.registerArrayType(String.class, "text");
        assertThat(handle.createQuery("select :array = :array")
                .bindArray("array", String.class, Collections.singletonList("element"))
                .mapTo(boolean.class)
                .one()).isTrue();
    }

    @Test
    public void testInstantArray() {
        ao.insertInstantArray(testInstants);
        assertThat(ao.fetchInstantArray()).containsExactly(testInstants);
    }

    @Test
    public void testInstantList() {
        ao.insertInstantList(Arrays.asList(testInstants));
        assertThat(ao.fetchInstantList()).contains(testInstants);
    }

    @Test
    public void testInstantArrayList() {
        ao.insertInstantList(Arrays.asList(testInstants));
        assertThat(ao.fetchInstantArrayList()).contains(testInstants);
    }

    @Test
    public void testInstantLinkedList() {
        ao.insertInstantList(Arrays.asList(testInstants));
        assertThat(ao.fetchInstantLinkedList()).contains(testInstants);
    }

    @Test
    public void testInstantCopyOnWriteArrayList() {
        ao.insertInstantList(Arrays.asList(testInstants));
        assertThat(ao.fetchInstantCopyOnWriteArrayList()).contains(testInstants);
    }

    public interface ArrayObject {
        @SqlQuery(U_SELECT)
        @SingleValue
        UUID[] fetchUuidArray();

        @SqlUpdate(U_INSERT)
        void insertUuidArray(UUID[] uuids);

        @SqlQuery(U_SELECT)
        @SingleValue
        List<UUID> fetchUuidList();

        @SqlQuery(U_SELECT)
        @SingleValue
        ArrayList<UUID> fetchUuidArrayList();

        @SqlQuery(U_SELECT)
        @SingleValue
        LinkedList<UUID> fetchUuidLinkedList();

        @SqlQuery(U_SELECT)
        @SingleValue
        CopyOnWriteArrayList<UUID> fetchUuidCopyOnWriteArrayList();

        @SqlUpdate(U_INSERT)
        void insertUuidList(List<UUID> uuids);

        @SqlQuery(I_SELECT)
        @SingleValue
        int[] fetchIntArray();

        @SqlQuery(I_SELECT)
        @SingleValue
        Integer[] fetchBoxedIntArray();

        @SqlQuery(I_SELECT)
        @SingleValue
        Object[] fetchObjectArray();

        @SqlUpdate(I_INSERT)
        void insertIntArray(int[] ints);

        @SqlUpdate(I_INSERT)
        void insertBoxedIntArray(Integer[] ints);

        @SqlQuery(I_SELECT)
        @SingleValue
        List<Integer> fetchIntList();

        @SqlUpdate(I_INSERT)
        void insertIntList(List<Integer> ints);

        @SqlQuery(T_SELECT)
        @SingleValue
        Instant[] fetchInstantArray();

        @SqlUpdate(T_INSERT)
        void insertInstantArray(Instant[] instants);

        @SqlQuery(T_SELECT)
        @SingleValue
        List<Instant> fetchInstantList();

        @SqlQuery(T_SELECT)
        @SingleValue
        ArrayList<Instant> fetchInstantArrayList();

        @SqlQuery(T_SELECT)
        @SingleValue
        LinkedList<Instant> fetchInstantLinkedList();

        @SqlQuery(T_SELECT)
        @SingleValue
        CopyOnWriteArrayList<Instant> fetchInstantCopyOnWriteArrayList();

        @SqlUpdate(T_INSERT)
        void insertInstantList(List<Instant> instants);

    }

    @Test
    public void testWhereInArray() {
        WhereInDao dao = handle.attach(WhereInDao.class);
        dao.createTable();
        Something a = new Something(1, "Alice");
        Something b = new Something(2, "Bob");
        Something c = new Something(3, "Candace");
        Something d = new Something(4, "David");
        Something e = new Something(5, "Emily");
        dao.insert(a, b, c, d, e);

        assertThat(dao.getByIds(1)).containsExactly(a);
        assertThat(dao.getByIds(2)).containsExactly(b);
        assertThat(dao.getByIds(3)).containsExactly(c);
        assertThat(dao.getByIds(4)).containsExactly(d);
        assertThat(dao.getByIds(5)).containsExactly(e);
        assertThat(dao.getByIds(1, 2, 5)) // Three, sir!
                .containsExactly(a, b, e);
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface WhereInDao {
        @SqlUpdate("create table something(id int, name text)")
        void createTable();

        @SqlBatch("insert into something(id, name) values (:id, :name)")
        void insert(@BindBean Something... somethings);

        @SqlQuery("select * from something where id = any(:ids) order by id")
        List<Something> getByIds(int... ids);
    }
}

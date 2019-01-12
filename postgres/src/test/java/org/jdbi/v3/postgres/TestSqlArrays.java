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
package org.jdbi.v3.postgres;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.SingleValue;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSqlArrays {
    private static final String U_SELECT = "SELECT u FROM uuids";
    private static final String U_INSERT = "INSERT INTO uuids VALUES(:uuids, NULL)";
    private static final String I_SELECT = "SELECT i FROM uuids";
    private static final String I_INSERT = "INSERT INTO uuids VALUES(NULL, :ints)";

    @ClassRule
    public static JdbiRule db = PostgresDbRule.rule();

    private Handle h;
    private ArrayObject ao;

    @Before
    public void setUp() {
        h = db.getHandle();
        h.useTransaction(th -> {
            th.execute("DROP TABLE IF EXISTS uuids");
            th.execute("CREATE TABLE uuids (u UUID[], i INT[])");
        });
        ao = h.attach(ArrayObject.class);
    }

    private final UUID[] testUuids = new UUID[] {
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
    };

    private final int[] testInts = new int[] {
        5, 4, -6, 1, 9, Integer.MAX_VALUE, Integer.MIN_VALUE
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
        assertThat(actuals).containsExactly(actuals);
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
    }

    @Test
    public void testWhereInArray() {
        WhereInDao dao = h.attach(WhereInDao.class);
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

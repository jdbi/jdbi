package org.jdbi.v3.pg;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.jdbi.v3.Handle;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestSqlArrays {
    private static final String U_SELECT = "SELECT u FROM uuids";
    private static final String U_INSERT = "INSERT INTO uuids VALUES(:uuids, NULL)";
    private static final String I_SELECT = "SELECT i FROM uuids";
    private static final String I_INSERT = "INSERT INTO uuids VALUES(NULL, :ints)";

    @Rule
    public PostgresDbRule db = new PostgresDbRule();
    private Handle h;
    private ArrayObject ao;

    @Before
    public void setUp() {
        h = db.getSharedHandle();
        h.execute("CREATE TABLE uuids (u UUID[], i INT[])");
        ao = h.attach(ArrayObject.class);
    }

    private final UUID[] testUuids = new UUID[] {
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
    };

    private final int[] testInts = new int[] {
        5, 4, -6, 1, 9, Integer.MAX_VALUE, Integer.MIN_VALUE
    };

    @Test
    public void testUuidArray() throws Exception {
        ao.insertUuidArray(testUuids);
        assertArrayEquals(testUuids, ao.fetchUuidArray());
    }

    @Test
    public void testUuidList() throws Exception {
        ao.insertUuidList(Arrays.asList(testUuids));
        assertEquals(Arrays.asList(testUuids), ao.fetchUuidList());
    }

    @Test
    public void testIntArray() throws Exception {
        ao.insertIntArray(testInts);
        assertArrayEquals(testInts, ao.fetchIntArray());
    }

    @Test
    public void testIntList() throws Exception {
        List<Integer> testIntList = new ArrayList<Integer>();
        Arrays.stream(testInts).forEach(testIntList::add);
        ao.insertIntList(testIntList);
        assertEquals(testIntList, ao.fetchIntList());
    }

    interface ArrayObject {
        @SqlQuery(U_SELECT)
        UUID[] fetchUuidArray();

        @SqlUpdate(U_INSERT)
        void insertUuidArray(UUID[] u);

        @SqlQuery(U_SELECT)
        List<UUID> fetchUuidList();

        @SqlUpdate(U_INSERT)
        void insertUuidList(List<UUID> u);


        @SqlQuery(I_SELECT)
        int[] fetchIntArray();

        @SqlQuery(I_SELECT)
        Integer[] fetchBoxedIntArray();

        @SqlUpdate(I_INSERT)
        void insertIntArray(int[] u);

        @SqlQuery(I_SELECT)
        List<Integer> fetchIntList();

        @SqlUpdate(I_INSERT)
        void insertIntList(List<Integer> u);
    }
}

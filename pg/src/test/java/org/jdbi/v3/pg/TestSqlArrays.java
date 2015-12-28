package org.jdbi.v3.pg;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
    private static final String SELECT = "SELECT u FROM uuids";
    private static final String INSERT = "INSERT INTO uuids VALUES(:uuids)";

    @Rule
    public PostgresDbRule db = new PostgresDbRule();
    private Handle h;
    private UuidObject uo;

    @Before
    public void setUp() {
        h = db.getSharedHandle();
        h.execute("CREATE TABLE uuids (u UUID[])");
        uo = h.attach(UuidObject.class);
    }

    private final UUID[] testUuids = new UUID[] {
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
    };

    @Test
    public void testArray() throws Exception {
        uo.insertArray(testUuids);
        assertArrayEquals(testUuids, uo.fetchArray());
    }

    @Test
    public void testList() throws Exception {
        uo.insertList(Arrays.asList(testUuids));
        assertEquals(testUuids, uo.fetchList());
    }

    interface UuidObject {
        @SqlQuery(SELECT)
        UUID[] fetchArray();

        @SqlUpdate(INSERT)
        void insertArray(UUID[] u);

        @SqlQuery(SELECT)
        List<UUID> fetchList();

        @SqlUpdate(INSERT)
        void insertList(List<UUID> u);
    }
}

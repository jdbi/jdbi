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

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.SqlUpdate;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestUuid {
    @Rule
    public PostgresDbRule db = new PostgresDbRule();

    public Handle h;

    @Before
    public void setupDbi() throws Exception {
        h = db.getJdbi().open();
        h.execute("CREATE TABLE foo (bar UUID)");
    }

    @After
    public void tearDown() throws Exception {
        h.close();
    }

    @Test
    public void testUuid() throws Exception {
        UUID u = UUID.randomUUID();
        h.createStatement("INSERT INTO foo VALUES (:uuid)")
            .bind("uuid", u)
            .execute();

        assertEquals(u, h.createQuery("SELECT * FROM foo").mapTo(UUID.class).findOnly());
    }

    @Test
    public void testUuidObject() throws Exception {
        final Set<UUID> expected = new HashSet<>();
        final UuidObject uo = h.attach(UuidObject.class);

        assertEquals(expected, uo.getUuids());

        UUID u = UUID.randomUUID();
        uo.insert(u);
        expected.add(u);

        assertEquals(expected, uo.getUuids());

        uo.insert(u);
        assertEquals(expected, uo.getUuids());

        u = UUID.randomUUID();
        uo.insert(u);
        expected.add(u);
        assertEquals(expected, uo.getUuids());
    }

    public interface UuidObject {
        @SqlUpdate("INSERT INTO foo VALUES(:uuid)")
        void insert(UUID uuid);

        @SqlQuery("SELECT * FROM foo")
        Set<UUID> getUuids();
    }
}

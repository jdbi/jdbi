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

import java.util.Set;
import java.util.UUID;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.SingleValue;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUuid {

    @ClassRule
    public static JdbiRule db = PostgresDbRule.rule();

    public Handle h;

    @Before
    public void setupDbi() throws Exception {
        h = db.getHandle();
        h.useTransaction(th -> {
            th.execute("DROP TABLE IF EXISTS foo");
            th.execute("CREATE TABLE foo (bar UUID, ary UUID[])");
        });
    }

    @Test
    public void testUuid() throws Exception {
        UUID u = UUID.randomUUID();
        h.createUpdate("INSERT INTO foo VALUES (:uuid)")
            .bind("uuid", u)
            .execute();

        assertThat(h.createQuery("SELECT * FROM foo").mapTo(UUID.class).findOnly()).isEqualTo(u);
    }

    @Test
    public void testUuidObject() throws Exception {
        final UuidObject uo = h.attach(UuidObject.class);

        assertThat(uo.getUuids()).isEmpty();

        UUID u = UUID.randomUUID();
        uo.insert(u);
        assertThat(uo.getUuids()).containsOnly(u);

        uo.insert(u);
        assertThat(uo.getUuids()).containsOnly(u);

        UUID u1 = UUID.randomUUID();
        uo.insert(u1);
        assertThat(uo.getUuids()).containsOnly(u, u1);
    }

    @Test
    public void testNull() throws Exception {
        final UuidObject uo = h.attach(UuidObject.class);

        uo.insert(null);

        assertThat(uo.getUuid()).isEqualTo(null);
    }

    @Test
    public void testUuidArray() throws Exception {
        final UuidObject uo = h.attach(UuidObject.class);

        UUID[] ary = new UUID[] {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};

        uo.insertArray(ary);

        assertThat(uo.getArray()).containsExactly(ary);
    }

    public interface UuidObject {
        @SqlUpdate("INSERT INTO foo (bar) VALUES(:uuid)")
        void insert(UUID uuid);

        @SqlQuery("SELECT bar FROM foo")
        Set<UUID> getUuids();

        @SqlQuery("select bar from foo")
        UUID getUuid();

        @SqlUpdate("insert into foo (ary) values (:uuids)")
        void insertArray(UUID... uuids);

        @SqlQuery("select ary from foo")
        @SingleValue
        UUID[] getArray();
    }
}

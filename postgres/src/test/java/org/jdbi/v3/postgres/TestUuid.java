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

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.SingleValue;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUuid {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withPlugins(new SqlObjectPlugin(), new PostgresPlugin())
        .withInitializer((ds, h) -> h.useTransaction(th -> {
            th.execute("DROP TABLE IF EXISTS foo");
            th.execute("CREATE TABLE foo (bar UUID, ary UUID[])");
        }));

    public Handle handle;

    @BeforeEach
    public void setupDbi() {
        handle = pgExtension.openHandle();
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    @Test
    public void testUuid() {
        UUID u = UUID.randomUUID();
        handle.createUpdate("INSERT INTO foo VALUES (:uuid)")
            .bind("uuid", u)
            .execute();

        assertThat(handle.createQuery("SELECT * FROM foo").mapTo(UUID.class).one()).isEqualTo(u);
    }

    @Test
    public void testUuidObject() {
        final UuidObject uo = handle.attach(UuidObject.class);

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
    public void testNull() {
        final UuidObject uo = handle.attach(UuidObject.class);

        uo.insert(null);

        assertThat(uo.getUuid()).isNull();
    }

    @Test
    public void testUuidArray() {
        final UuidObject uo = handle.attach(UuidObject.class);

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

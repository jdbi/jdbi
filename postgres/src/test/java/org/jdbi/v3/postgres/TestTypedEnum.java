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

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestTypedEnum {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withPlugins(new SqlObjectPlugin(), new PostgresPlugin())
        .withInitializer((ds, h) -> h.useTransaction(th -> {
            th.execute("DROP TABLE IF EXISTS values");
            th.execute("DROP TYPE IF EXISTS enum_t");
            th.execute("CREATE TYPE enum_t AS ENUM ('FOO', 'BAR', 'BAZ')");
            th.execute("CREATE TABLE values (value enum_t)");
        }));

    public Handle handle;

    @BeforeEach
    public void setupDbi() {
        handle = pgExtension.openHandle();
    }

    @Test
    public void testBind() {
        handle.createUpdate("INSERT INTO values VALUES(:value)")
            .bind("value", EnumT.BAR)
            .execute();

        assertThat(handle.createQuery("SELECT * FROM values").mapTo(String.class).one())
            .isEqualTo("BAR");
    }

    @Test
    public void testMap() {
        handle.createUpdate("INSERT INTO values VALUES('BAZ')")
            .execute();

        assertThat(handle.createQuery("SELECT * FROM values").mapTo(EnumT.class).one())
            .isEqualTo(EnumT.BAZ);
    }

    public enum EnumT {
        FOO, BAR, BAZ
    }
}

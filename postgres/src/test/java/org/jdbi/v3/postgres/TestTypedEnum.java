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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestTypedEnum {
    @ClassRule
    public static JdbiRule db = PostgresDbRule.rule();

    public Handle h;

    @Before
    public void setupDbi() {
        h = db.getHandle();
        h.useTransaction(th -> {
            th.execute("DROP TABLE IF EXISTS values");
            th.execute("DROP TYPE IF EXISTS enum_t");
            th.execute("CREATE TYPE enum_t AS ENUM ('FOO', 'BAR', 'BAZ')");
            th.execute("CREATE TABLE values (value enum_t)");
        });
    }

    @Test
    public void testBind() {
        h.createUpdate("INSERT INTO values VALUES(:value)")
            .bind("value", EnumT.BAR)
            .execute();

        assertThat(h.createQuery("SELECT * FROM values").mapTo(String.class).one())
            .isEqualTo("BAR");
    }

    @Test
    public void testMap() {
        h.createUpdate("INSERT INTO values VALUES('BAZ')")
            .execute();

        assertThat(h.createQuery("SELECT * FROM values").mapTo(EnumT.class).one())
            .isEqualTo(EnumT.BAZ);
    }

    public enum EnumT {
        FOO, BAR, BAZ
    }
}

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
package org.jdbi.core.statement;

import java.sql.Types;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import de.softwareforge.testing.postgres.junit5.RequirePostgresVersion;
import org.assertj.core.data.Offset;
import org.jdbi.core.Handle;
import org.jdbi.core.junit5.PgDatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@RequirePostgresVersion(atLeast = "14")
public class TestCallable {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public PgDatabaseExtension pgExtension = PgDatabaseExtension.instance(pg);

    private Handle h;

    @BeforeEach
    public void setUp() {
        h = pgExtension.getSharedHandle();
        h.execute("CREATE OR REPLACE PROCEDURE TO_DEGREES(OUT result float, value float) AS $$\n"
            + "BEGIN\n"
            + "result := DEGREES(value);\n"
            + "END;\n"
            + "$$ LANGUAGE plpgsql;");

        h.execute("CREATE OR REPLACE PROCEDURE DO_LENGTH(value varchar, OUT result integer) AS $$\n"
            + "BEGIN\n"
            + "IF value IS NULL THEN\n"
            + "result := NULL;\n"
            + "ELSE\n"
            + "result := CHAR_LENGTH(value);\n"
            + "END IF;\n"
            + "END;\n"
            + "$$ LANGUAGE plpgsql;");

        h.execute("CREATE OR REPLACE PROCEDURE WITH_SIDE_EFFECT(v1 integer, v2 varchar) AS $$\n"
            + "BEGIN\n"
            + "INSERT INTO something (id, name) VALUES (v1, v2 || ' Doe');"
            + "END;\n"
            + "$$ LANGUAGE plpgsql;");

        h.execute("CREATE TABLE something (id integer not null primary key, name varchar(255))");
    }

    @Test
    public void testStatement() {
        try (Call call = h.createCall("CALL TO_DEGREES(?, ?)")) {
            OutParameters ret = call.registerOutParameter(0, Types.DOUBLE)
                .bind(1, 100.0d)
                .invoke();

            Double expected = Math.toDegrees(100.0d);
            assertThat(ret.getDoubleValue(0)).isEqualTo(expected, Offset.offset(0.001));
            assertThat(ret.getLongValue(0)).isEqualTo(expected.longValue());
            assertThat(ret.getShortValue(0)).isEqualTo(expected.shortValue());
            assertThat(ret.getIntValue(0)).isEqualTo(expected.intValue());
            assertThat(ret.getFloatValue(0)).isEqualTo(expected.floatValue(), Offset.offset(0.001f));

            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> ret.getDate(0));
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> ret.getDate(1));
        }
    }

    @Test
    public void testStatementWithNamedParam() {
        try (Call call = h.createCall("CALL TO_DEGREES(:x, :y)")) {
            OutParameters ret = call
                .registerOutParameter("x", Types.DOUBLE)
                .bind("y", 100.0d)
                .invoke();

            Double expected = Math.toDegrees(100.0d);
            assertThat(ret.getDoubleValue("x")).isEqualTo(expected, Offset.offset(0.001));
            assertThat(ret.getLongValue("x")).isEqualTo(expected.longValue());
            assertThat(ret.getShortValue("x")).isEqualTo(expected.shortValue());
            assertThat(ret.getIntValue("x")).isEqualTo(expected.intValue());
            assertThat(ret.getFloatValue("x")).isEqualTo(expected.floatValue());

            assertThatExceptionOfType(Exception.class).isThrownBy(() -> ret.getDate("x"));
            assertThatExceptionOfType(Exception.class).isThrownBy(() -> ret.getDate("y"));
        }
    }

    @Test
    public void testWithNullReturn() {
        try (Call call = h.createCall("CALL DO_LENGTH(?, ?)")) {
            OutParameters ret = call.bind(0, (String) null)
                .registerOutParameter(1, Types.INTEGER)
                .invoke();

            Integer out = ret.getInt(1);
            assertThat(out).isNull();
        }
    }

    @Test
    public void testWithNullReturnWithNamedParam() {
        try (Call call = h.createCall("CALL DO_LENGTH(:in, :out)")) {
            OutParameters ret = call.bindNull("in", Types.VARCHAR)
                .registerOutParameter("out", Types.INTEGER)
                .invoke();

            Integer out = ret.getInt(1);
            assertThat(out).isNull();
        }
    }

    @Test
    public void testProcedureWithoutOutParameter() {
        try (Call call = h.createCall("CALL WITH_SIDE_EFFECT(:id, :name)")) {
            call.bind("id", 10)
                .bind("name", "John")
                .invoke();

            String name = h.createQuery("SELECT name FROM something WHERE id = :id")
                .bind("id", 10)
                .mapTo(String.class)
                .one();

            assertThat(name).isEqualTo("John Doe");
        }
    }

    @Test
    public void testNullValue() {
        h.execute("CREATE OR REPLACE PROCEDURE RETURN_VALUE(input integer, OUT result integer) AS $$\n"
            + "BEGIN\n"
            + "result := input;\n"
            + "END;\n"
            + "$$ LANGUAGE plpgsql;");

        try (Call call = h.createCall("CALL RETURN_VALUE(:in, :out)")) {
            OutParameters result = call.bind("in", 10)
                .registerOutParameter("out", Types.INTEGER)
                .invoke();

            int out = result.getIntValue("out");
            assertThat(out).isEqualTo(10);
        }

        try (Call call = h.createCall("CALL RETURN_VALUE(:in, :out)")) {
            OutParameters result = call.bindNull("in", Types.INTEGER)
                .registerOutParameter("out", Types.INTEGER)
                .invoke();

            int out = result.getIntValue("out");
            assertThat(out).isZero();
        }

        try (Call call = h.createCall("CALL RETURN_VALUE(:in, :out)")) {
            OutParameters result = call.bindNull("in", Types.INTEGER)
                .registerOutParameter("out", Types.INTEGER)
                .invoke();

            Integer out = result.getInt("out");
            assertThat(out).isNull();
        }
    }
}

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
package org.jdbi.testcontainers;

import java.sql.Types;
import java.util.List;

import org.jdbi.core.Handle;
import org.jdbi.core.statement.Call;
import org.jdbi.core.statement.OutParameters;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@Testcontainers
class TestMSSQLStoredProcedure {

    @Container
    static JdbcDatabaseContainer<?> dbContainer = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
        .acceptLicense();

    @RegisterExtension
    JdbiExtension extension = JdbiTestcontainersExtension.instance(dbContainer)
        .withPlugin(new SqlObjectPlugin());

    @Test
    void testMsSqlServerCall() {
        Handle h = extension.getSharedHandle();
        h.execute("""
                CREATE PROCEDURE simpleCursor
                AS
                BEGIN
                SELECT 'hello' UNION ALL SELECT 'world'
                END;""");

        try (Call call = h.createCall("{ call simpleCursor()}")) {
            OutParameters output = call.invoke();

            List<String> results = output.getResultSet().mapTo(String.class).list();
            assertThat(results).isNotEmpty().containsExactly("hello", "world");
        }
    }

    @Test
    void testMsSqlOutParameter() {
        Handle h = extension.getSharedHandle();
        h.execute("""
                CREATE PROCEDURE passValues
                @inValue NVARCHAR(64),
                @outValue1 NVARCHAR(64) OUTPUT,
                @outValue2 NVARCHAR(64) OUTPUT
                AS
                BEGIN
                SELECT @outValue1 = @inValue;
                SELECT @outValue2 = 'hello';
                END;""");

        try (Call call = h.createCall("{ call passValues(?, ?, ?)}")) {
            call.registerOutParameter(1, Types.NVARCHAR)
                .registerOutParameter(2, Types.NVARCHAR)
                .bind(0, "input value");
            OutParameters output = call.invoke();
            assertThat(output.getString(1)).isEqualTo("input value");
            assertThat(output.getString(2)).isEqualTo("hello");
        }
    }

    @Test
    void testMsSqlOutParametersAndResultSet() {
        Handle h = extension.getSharedHandle();
        h.execute("""
                CREATE PROCEDURE passValues
                @inValue NVARCHAR(64),
                @outValue1 NVARCHAR(64) OUTPUT,
                @outValue2 NVARCHAR(64) OUTPUT
                AS
                BEGIN
                SELECT @outValue1 = @inValue;
                SELECT @outValue2 = 'hello';
                SELECT 'hello' UNION ALL SELECT 'world'
                END;""");

        try (Call call = h.createCall("{ call passValues(?, ?, ?)}")) {
            call.registerOutParameter(1, Types.NVARCHAR)
                .registerOutParameter(2, Types.NVARCHAR)
                .bind(0, "input value");
            OutParameters output = call.invoke();
            List<String> results = output.getResultSet().mapTo(String.class).list();
            assertThat(results).isNotEmpty().containsExactly("hello", "world");

            assertThat(output.getString(1)).isEqualTo("input value");
            assertThat(output.getString(2)).isEqualTo("hello");
        }
    }

    @Test
    void testMsSqlDocExample() {
        Handle h = extension.getSharedHandle();
        h.execute("""
                CREATE PROCEDURE mssql_add
                @a INT,
                @b INT
                AS
                BEGIN
                SELECT @a + @b;
                END;""");

        try (Call call = h.createCall("{ call mssql_add(:a, :b)}")) {
            call.bind("a", 13)
                .bind("b", 9);
            OutParameters output = call.invoke();

            int sum = output.getResultSet().mapTo(Integer.class).one();
            assertThat(sum).isEqualTo(22);
        }
    }

}

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
package org.jdbi.v3.opentelemetry;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class TestStatusCode {
    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
        "postgresql, 23505, 0, 23505",
        "mysql, 23000, 1062, 1062",
        "mysql, 08S01, 0, 08S01",
        "mariadb, 23000, 1062, 1062",
        "microsoft.sql_server, 23000, 2627, 2627",
        "oracle.db, 23000, 1, ORA-00001",
        "oracle.db, null, 17002, ORA-17002",
        "sqlite, null, 19, 19",
        "h2database, 23505, 23505, 23505",
        "h2database, null, 0, null",
    })
    void statusCode(final String system, final String sqlState, final int errorCode, final String expected) {
        assertThat(JdbiOpenTelemetryPlugin.statusCode(system, new SQLException("message", sqlState, errorCode)))
            .isEqualTo(expected);
    }

    @Test
    void findSqlExceptionInCauseChain() {
        final var sqlException = new SQLException("message", "23505");
        final var wrapped = new IllegalStateException(new RuntimeException(sqlException));

        assertThat((Throwable) JdbiOpenTelemetryPlugin.findSqlException(wrapped)).isSameAs(sqlException);
        assertThat((Throwable) JdbiOpenTelemetryPlugin.findSqlException(new IllegalStateException())).isNull();
    }
}

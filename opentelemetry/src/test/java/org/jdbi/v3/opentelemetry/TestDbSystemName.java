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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDbSystemName {
    @ParameterizedTest
    @CsvSource({
        "PostgreSQL, postgresql",
        "MySQL, mysql",
        "MariaDB, mariadb",
        "Microsoft SQL Server, microsoft.sql_server",
        "Oracle, oracle.db",
        "H2, h2database",
        "HSQL Database Engine, hsqldb",
        "SQLite, sqlite",
        "Apache Derby, derby",
        "DB2/LINUXX8664, ibm.db2",
        "Firebird 4.0, firebirdsql",
        "HDB, sap.hana",
        "Snowflake, snowflake",
    })
    void productName(final String productName, final String expected) {
        assertThat(JdbiOpenTelemetryPlugin.dbSystemName(productName)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void unknownProduct(final String productName) {
        assertThat(JdbiOpenTelemetryPlugin.dbSystemName(productName)).isEqualTo(JdbiOpenTelemetryPlugin.OTHER_SQL);
    }
}

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
package org.jdbi.v3.testing.junit5.tc;

import org.junit.jupiter.api.Tag;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("slow")
@Testcontainers
class ClickhouseJdbiTestContainerExtensionTest extends AbstractJdbiTestcontainersExtensionTest {

    @Container
    static JdbcDatabaseContainer<?> dbContainer = new ClickHouseContainer("clickhouse/clickhouse-server:latest");

    @Override
    JdbcDatabaseContainer<?> getDbContainer() {
        return dbContainer;
    }

    @Override
    protected String getTableCreateStatement() {
        // I'm different!
        return super.getTableCreateStatement() + " Engine = Memory";
    }
}

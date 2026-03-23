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
package org.jdbi.v3.mysql;

import org.jdbi.v3.sqlobject.AbstractSqlObjectJavaTimeTests;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("slow")
@Testcontainers
public class TestMysqlSqlObjectJavaTime extends AbstractSqlObjectJavaTimeTests {

    static final String MYSQL_VERSION = System.getProperty("jdbi.test.mysql-version", "mysql");

    @Container
    static JdbcDatabaseContainer<?> dbContainer = new MySQLContainer<>(MYSQL_VERSION);

    @RegisterExtension
    JdbiExtension extension = JdbiTestcontainersExtension.instance(dbContainer)
        .withPlugins(new MysqlPlugin(), new SqlObjectPlugin());

    @BeforeEach
    public void setUp() {
        handle = extension.openHandle();
        handle.useTransaction(th -> {
            th.execute("drop table if exists stuff");
            th.execute("create table stuff (ts timestamp(6), d date, t time(6), z varchar(64))");
        });

        dao = handle.attach(TimeDao.class);
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    /**
     * MySQL has no timestamp with time zone
     */
    @Override
    protected boolean skipTSTZ() {
        return true;
    }
}

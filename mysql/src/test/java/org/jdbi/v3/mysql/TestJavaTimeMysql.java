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

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.assertj.core.data.TemporalUnitOffset;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.sqlobject.AbstractJavaTimeSqlObjectTests;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("slow")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestJavaTimeMysql {

    static final String MYSQL_VERSION = System.getProperty("jdbi.test.mysql-version", "mysql");

    @Container
    static JdbcDatabaseContainer<?> dbContainer = new MySQLContainer<>(MYSQL_VERSION);

    @RegisterExtension
    JdbiExtension extension = JdbiTestcontainersExtension.instance(dbContainer)
        .withPlugin(new SqlObjectPlugin());

    @RegisterExtension
    JdbiExtension extensionPlugin = JdbiTestcontainersExtension.instance(dbContainer)
        .withPlugins(new MysqlPlugin(), new SqlObjectPlugin());

    @BeforeAll
    public void setUp() {
        for (var ext : List.of(extension, extensionPlugin)) {
            try (var h = ext.openHandle()) {
                h.useTransaction(th -> {
                    th.execute("drop table if exists time_test");
                    th.execute("""
                            create table time_test (
                            ts timestamp(6),
                            d date,
                            t time(6),
                            dt datetime(6))
                        """);
                });
            }
        }
    }

    @BeforeEach
    public void cleanUp() {
        for (var ext : List.of(extension, extensionPlugin)) {
            try (var h = ext.openHandle()) {
                h.execute("delete from time_test");
            }
        }
    }

    // MySQL has no timezone support. Without either the plugin loaded or
    // in legacy mode, MySQL will not work with any timezone related classes
    private static final Map<Class<?>, String[]> MYSQL_STANDARD_TESTS = Map.of(
        // non-timezone types
        Instant.class, new String[] { "ts", "dt" },
        Timestamp.class, new String[] { "ts", "dt" },
        LocalDateTime.class, new String[] { "ts", "dt" },

        Time.class, new String[] { "t" },
        LocalTime.class, new String[] { "t" },

        Date.class, new String[] { "d", "dt" },
        LocalDate.class, new String[] { "d", "dt" }
    );

    // MySQL has no timezone support
    private static final Map<Class<?>, String[]> MYSQL_EXTENDED_TESTS = Map.of(
        OffsetDateTime.class, new String[] { "ts", "dt" },
        ZonedDateTime.class, new String[] { "ts", "dt" },
        OffsetTime.class, new String[] { "t" },

        // non-timezone types
        Instant.class, new String[] { "ts", "dt" },
        Timestamp.class, new String[] { "ts", "dt" },
        LocalDateTime.class, new String[] { "ts", "dt" },

        Time.class, new String[] { "t" },
        LocalTime.class, new String[] { "t" },

        Date.class, new String[] { "d", "dt" },
        LocalDate.class, new String[] { "d", "dt" }
    );


    abstract class AbstractMysqlTests extends AbstractJavaTimeSqlObjectTests {

        protected AbstractMysqlTests(Map<Class<?>, String[]> tests) {
            super(tests);
        }

        protected Handle getHandle() {
            return extension.openHandle();
        }
    }

    @Nested
    class TestStandard extends AbstractMysqlTests {

        TestStandard() {
            super(MYSQL_STANDARD_TESTS);
        }
    }

    @Nested
    class TestLegacy extends AbstractMysqlTests {
        TestLegacy() {
            super(MYSQL_EXTENDED_TESTS);
        }

        // Turn on legacy testing

        @Override
        protected <T> QualifiedType<T> getTestType(Class<T> clazz) {
            return super.getLegacyTestType(clazz);
        }

        @Override
        protected TemporalUnitOffset getAllowableOffset(TestResult result) {
            return super.getLegacyAllowableOffset(result);
        }

        @Override
        protected ZoneOffset getExpectedZoneOffset(Class<?> clazz, Object value, String column) {
            return super.getLegacyExpectedZoneOffset(clazz);
        }
    }

    @Nested
    class TestPlugin extends AbstractMysqlTests {
        TestPlugin() {
            super(MYSQL_EXTENDED_TESTS);
        }

        @Override
        protected Handle getHandle() {
            return extensionPlugin.openHandle();
        }

        @Override
        protected ZoneOffset getExpectedZoneOffset(Class<?> clazz, Object value, String column) {
            return getSystemDefaultOffset();
        }
    }

    @Nested
    class TestPluginLegacy extends TestLegacy {
        @Override
        protected Handle getHandle() {
            return extensionPlugin.openHandle();
        }
    }
}

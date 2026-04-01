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
package org.jdbi.v3.testcontainers.mssql;

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
import java.time.temporal.ChronoUnit;
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
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.within;

@Tag("slow")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestJavaTimeMssql {

    @Container
    static JdbcDatabaseContainer<?> dbContainer = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
        .acceptLicense();

    @RegisterExtension
    JdbiExtension extension = JdbiTestcontainersExtension.instance(dbContainer)
        .withPlugin(new SqlObjectPlugin());

    @BeforeAll
    public void setUp() {
        try (var h = extension.openHandle()) {
            h.useTransaction(th -> {
                th.execute("drop table if exists time_test");
                th.execute("""
                    create table time_test (
                        ts datetime2,
                        d date,
                        t time,
                        dt datetime,
                        tstz datetimeoffset)
                    """);
            });
        }
    }

    @BeforeEach
    public void cleanUp() {
        try (var h = extension.openHandle()) {
            h.execute("delete from time_test");
        }
    }

    // MS-SQL is even weirder than MySQL...
    private static final Map<Class<?>, String[]> MSSQL_STANDARD_TESTS = Map.of(
        OffsetDateTime.class, new String[] { "tstz" },
        ZonedDateTime.class, new String[] { "tstz" },
        OffsetTime.class, new String[] { "tstz" },

        // non-timezone types
        Instant.class, new String[] { "ts", "dt" },
        Timestamp.class, new String[] { "ts", "dt" },
        LocalDateTime.class, new String[] { "ts", "dt" },

        Time.class, new String[] { "t" },
        LocalTime.class, new String[] { "t" },

        Date.class, new String[] { "d", "dt" },
        LocalDate.class, new String[] { "d", "dt" }
    );

    // legacy mapping does not work with datetimeoffset
    private static final Map<Class<?>, String[]> MSSQL_LEGACY_TESTS = Map.of(
        OffsetDateTime.class, new String[] { "ts" },
        ZonedDateTime.class, new String[] { "ts" },
        OffsetTime.class, new String[] { "ts", "t", "dt" },

        // non-timezone types
        Instant.class, new String[] { "ts", "dt" },
        Timestamp.class, new String[] { "ts", "dt" },
        LocalDateTime.class, new String[] { "ts", "dt" },

        Time.class, new String[] { "t" },
        LocalTime.class, new String[] { "t" },

        Date.class, new String[] { "d", "dt" },
        LocalDate.class, new String[] { "d", "dt" }
    );


    abstract class AbstractMssqlTests extends AbstractJavaTimeSqlObjectTests {

        protected AbstractMssqlTests(Map<Class<?>, String[]> tests) {
            super(tests);
        }

        @Override
        protected Handle getHandle() {
            return extension.openHandle();
        }

        // MS-SQL datetime only supports weird precision - 0.00333 second and time is even worse.
        @Override
        protected TemporalUnitOffset getAllowableOffset(TestResult result) {
            if ("dt".equals(result.columnName())) {
                return within(10, ChronoUnit.MILLIS);
            } else if (result.type() == LocalTime.class) {
                return within(2, ChronoUnit.MILLIS);
            } else {
                return super.getAllowableOffset(result);
            }
        }
    }

    @Nested
    class TestStandard extends AbstractMssqlTests {
        TestStandard() {
            super(MSSQL_STANDARD_TESTS);
        }
    }

    @Nested
    class TestLegacy extends AbstractMssqlTests {
        TestLegacy() {
            super(MSSQL_LEGACY_TESTS);
        }

        // Turn on legacy testing

        @Override
        protected <T> QualifiedType<T> getTestType(Class<T> clazz) {
            return super.getLegacyTestType(clazz);
        }

        @Override
        // MS-SQL datetime only supports weird precision - 0.00333 second
        protected TemporalUnitOffset getAllowableOffset(TestResult result) {
            if (result.type() == OffsetTime.class || result.type() == LocalTime.class) {
                return super.getLegacyAllowableOffset(result);
            } else if ("dt".equals(result.columnName())) {
                return within(10, ChronoUnit.MILLIS);
            } else {
                return super.getLegacyAllowableOffset(result);
            }
        }

        @Override
        protected ZoneOffset getExpectedZoneOffset(Class<?> clazz, Object value, String column) {
            return super.getLegacyExpectedZoneOffset(clazz);
        }
    }
}

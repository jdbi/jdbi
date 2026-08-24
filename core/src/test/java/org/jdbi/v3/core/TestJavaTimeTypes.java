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
package org.jdbi.v3.core;

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
import java.util.Map;

import org.assertj.core.data.TemporalUnitOffset;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * This is nearly identical to TestJavaTimeH2 in sqlobject; this is intentional:
 * <br>
 * This class catches regressions quickly. Everything else (pg, mysql, mssql, oracle)
 * either runs later (pg after sqlobject) or only in slow tests (mysql, mssql, oracle).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestJavaTimeTypes {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @BeforeEach
    public void setUp() {
        try (var h = h2Extension.openHandle()) {
            h.useTransaction(th -> {
                th.execute("drop table if exists time_test");
                th.execute("""
                        create table time_test (
                        ts timestamp,
                        tstz timestamp with time zone,
                        d date,
                        t time(6),
                        ttz time(6) with time zone)
                    """);
            });
        }
    }

    // H2 supports Time/LocalTime mapped to a timestamp but no datetime.
    private static final Map<Class<?>, String[]> H2_STANDARD_TESTS = Map.of(
        OffsetDateTime.class, new String[] { "tstz", "ts" },
        ZonedDateTime.class, new String[] { "tstz", "ts" },
        OffsetTime.class, new String[] { "tstz", "ttz" },

        // non-timezone types
        Instant.class, new String[] { "tstz", "ts" },
        Timestamp.class, new String[] { "tstz", "ts" },
        LocalDateTime.class, new String[] { "ts" },

        Time.class, new String[] { "t", "ts" },
        LocalTime.class, new String[] { "t", "ts" },

        Date.class, new String[] { "d" },
        LocalDate.class, new String[] { "d" }
    );

    // H2 supports Time/LocalTime mapped to a timestamp but no datetime.
    private static final Map<Class<?>, String[]> H2_LEGACY_TESTS = Map.of(
        OffsetDateTime.class, new String[] { "tstz", "ts" },
        ZonedDateTime.class, new String[] { "tstz", "ts" },
        OffsetTime.class, new String[] { "tstz", "ttz", "t", "ts" },

        // non-timezone types
        Instant.class, new String[] { "tstz", "ts" },
        Timestamp.class, new String[] { "ts" },
        LocalDateTime.class, new String[] { "ts" },

        Time.class, new String[] { "t", "ts" },
        LocalTime.class, new String[] { "ttz", "t", "ts" },

        Date.class, new String[] { "d" },
        LocalDate.class, new String[] { "d" }
    );

    abstract class AbstractH2Tests extends AbstractJavaTimeTests {

        protected AbstractH2Tests(Map<Class<?>, String[]> tests) {
            super(tests);
        }

        protected Handle getHandle() {
            return h2Extension.openHandle();
        }

        @Override
        protected ZoneOffset getExpectedZoneOffset(Class<?> clazz, Object value, String column) {
            if ("ts".equals(column) || "t".equals(column)) {
                return getSystemDefaultOffset();
            } else {
                return super.getExpectedZoneOffset(clazz, value, column);
            }
        }
    }

    @Nested
    class TestStandard extends AbstractH2Tests {
        TestStandard() {
            super(H2_STANDARD_TESTS);
        }
    }

    @Nested
    class TestLegacy extends AbstractH2Tests {
        TestLegacy() {
            super(H2_LEGACY_TESTS);
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
}

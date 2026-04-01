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

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.assertj.core.data.TemporalUnitOffset;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.sqlobject.AbstractJavaTimeSqlObjectTests;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestJavaTimePg {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg)
        .withPlugin(new SqlObjectPlugin());

    @RegisterExtension
    public JdbiExtension pgExtensionPlugin = JdbiExtension.postgres(pg)
        .withPlugins(new PostgresPlugin(), new SqlObjectPlugin());

    @BeforeAll
    public void setUp() {
        for (var extension : List.of(pgExtension, pgExtensionPlugin)) {
            try (var h = extension.openHandle()) {
                h.useTransaction(th -> {
                    th.execute("drop table if exists time_test");
                    th.execute("""
                            create table time_test (
                            ts timestamp,
                            tstz timestamp with time zone,
                            d date,
                            t time,
                            ttz time with time zone)
                        """);
                });
            }
        }
    }

    @BeforeEach
    public void cleanUp() {
        for (var extension : List.of(pgExtension, pgExtensionPlugin)) {
            try (var h = extension.openHandle()) {
                h.execute("delete from time_test");
            }
        }
    }

    // Standard Postgres JDBC does not support TIMES WITH TIME ZONE as
    // a JDBC datatype or OffsetTime to TIMESTAMP WITH TIME ZONE
    private static final Map<Class<?>, String[]> PG_STANDARD_TESTS = Map.of(
        OffsetDateTime.class, new String[] { "tstz" },
        ZonedDateTime.class, new String[] { "tstz" },

        // non-timezone types
        Instant.class, new String[] { "tstz", "ts" },
        Timestamp.class, new String[] { "ts" },
        LocalDateTime.class, new String[] { "ts" },

        Time.class, new String[] { "t" },
        LocalTime.class, new String[] { "t" },

        Date.class, new String[] { "d" },
        LocalDate.class, new String[] { "d" }
    );

    // Legacy Mappers do not support OffsetTime to TIMESTAMP WITH TIME ZONE
    private static final Map<Class<?>, String[]> PG_LEGACY_TESTS = Map.of(
        OffsetDateTime.class, new String[] { "tstz", "ts" },
        ZonedDateTime.class, new String[] { "tstz", "ts" },
        OffsetTime.class, new String[] { "ttz" },

        // non-timezone types
        Instant.class, new String[] { "tstz", "ts" },
        Timestamp.class, new String[] { "ts" },
        LocalDateTime.class, new String[] { "ts" },

        Time.class, new String[] { "t" },
        LocalTime.class, new String[] { "ttz", "t" },

        Date.class, new String[] { "d" },
        LocalDate.class, new String[] { "d" }
    );

    abstract class AbstractPgTests extends AbstractJavaTimeSqlObjectTests {

        protected AbstractPgTests(Map<Class<?>, String[]> tests) {
            super(tests);
        }

        protected Handle getHandle() {
            return pgExtension.openHandle();
        }

        @Override
        protected ZoneOffset getExpectedZoneOffset(Class<?> clazz, Object value, String column) {
            if ("ts".equals(column) || "tstz".equals(column)) {
                // Postgres always returns UTC for TIMESTAMP and TIMESTAMP WITH TIME ZONE ... ¯\_(ツ)_/¯
                return ZoneOffset.UTC;
            } else {
                return super.getExpectedZoneOffset(clazz, value, column);
            }
        }
    }

    @Nested
    class TestStandard extends AbstractPgTests {
        TestStandard() {
            super(PG_STANDARD_TESTS);
        }
    }

    @Nested
    class TestLegacy extends AbstractPgTests {
        TestLegacy() {
            super(PG_LEGACY_TESTS);
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
    class TestPlugin extends AbstractPgTests {
        TestPlugin() {
            super(PG_STANDARD_TESTS);
        }

        @Override
        protected Handle getHandle() {
            return pgExtensionPlugin.openHandle();
        }
    }

    @Nested
    class TestPluginLegacy extends TestLegacy {
        @Override
        protected Handle getHandle() {
            return pgExtensionPlugin.openHandle();
        }
    }
}

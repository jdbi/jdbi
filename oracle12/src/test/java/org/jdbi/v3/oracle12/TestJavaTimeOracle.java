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
package org.jdbi.v3.oracle12;

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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

@Tag("slow")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestJavaTimeOracle {

    static final String CONTAINER_VERSION = "gvenzl/oracle-free:" + System.getProperty("oracle.container.version", "slim-faststart");

    @Container
    static OracleContainer oc = new OracleContainer(CONTAINER_VERSION);

    @RegisterExtension
    public JdbiExtension extension = JdbiTestcontainersExtension.instance(oc)
        .withPlugin(new SqlObjectPlugin());

    @RegisterExtension
    public JdbiExtension extensionPlugin = JdbiTestcontainersExtension.instance(oc)
        .withPlugins(new OraclePlugin(), new SqlObjectPlugin());

    @BeforeAll
    public void setUp() {
        for (var ext : List.of(extension, extensionPlugin)) {
            try (var h = ext.openHandle()) {
                h.useTransaction(th -> {
                    th.execute("drop table if exists time_test");
                    th.execute("""
                            create table time_test (
                            ts timestamp,
                            tstz timestamp with time zone,
                            d date)
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

    private static final Map<Class<?>, String[]> ORACLE_STANDARD_TESTS = Map.of(
        OffsetDateTime.class, new String[] { "tstz" },
        ZonedDateTime.class, new String[] { "tstz" },
        OffsetTime.class, new String[] { "tstz" },

        // non-timezone types
        Instant.class, new String[] { "tstz", "ts" },
        Timestamp.class, new String[] { "ts" },
        LocalDateTime.class, new String[] { "ts" },

        Time.class, new String[] { "ts" },

        Date.class, new String[] { "d" },
        LocalDate.class, new String[] { "d" }
    );

    private static final Map<Class<?>, String[]> ORACLE_LEGACY_TESTS = Map.of(
        OffsetDateTime.class, new String[] { "tstz", "ts" },
        ZonedDateTime.class, new String[] { "tstz", "ts" },
        OffsetTime.class, new String[] { "tstz", "ts" },

        // non-timezone types
        Instant.class, new String[] { "tstz", "ts" },
        Timestamp.class, new String[] { "ts" },
        LocalDateTime.class, new String[] { "ts" },

        Time.class, new String[] { "ts" },
        LocalTime.class, new String[] { "ts" },

        Date.class, new String[] { "d" },
        LocalDate.class, new String[] { "d" }
    );

    abstract class AbstractOracleTests extends AbstractJavaTimeSqlObjectTests {

        protected AbstractOracleTests(Map<Class<?>, String[]> tests) {
            super(tests);
        }

        protected Handle getHandle() {
            return extension.openHandle();
        }
    }

    @Nested
    class TestStandard extends AbstractOracleTests {
        TestStandard() {
            super(ORACLE_STANDARD_TESTS);
        }
    }

    @Nested
    class TestLegacy extends AbstractOracleTests {
        TestLegacy() {
            super(ORACLE_LEGACY_TESTS);
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
    class TestPlugin extends AbstractOracleTests {
        TestPlugin() {
            super(ORACLE_STANDARD_TESTS);
        }

        @Override
        protected Handle getHandle() {
            return extensionPlugin.openHandle();
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


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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.array.AbstractJavaTimeArrayTests;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the {@code java.time} array battery against Postgres, plus the Postgres-specific cases:
 * {@code interval} element types, BC dates and years past 9999, the infinity endpoints, and the
 * server-side rounding of sub-microsecond precision.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestJavaTimeSqlArrays extends AbstractJavaTimeArrayTests {

    private static final Map<Class<?>, String> PG_ARRAY_TESTS = Map.of(
        LocalDate.class, "d",
        LocalTime.class, "t",
        LocalDateTime.class, "ts",
        OffsetDateTime.class, "tstz",
        OffsetTime.class, "ttz",
        Instant.class, "tstz",
        ZonedDateTime.class, "tstz",
        Duration.class, "iv",
        Period.class, "iv");

    private static final GenericType<List<LocalDate>> LOCAL_DATE_LIST = new GenericType<>() {};
    private static final GenericType<List<LocalTime>> LOCAL_TIME_LIST = new GenericType<>() {};
    private static final GenericType<List<LocalDateTime>> LOCAL_DATE_TIME_LIST = new GenericType<>() {};
    private static final GenericType<List<OffsetDateTime>> OFFSET_DATE_TIME_LIST = new GenericType<>() {};
    private static final GenericType<List<Duration>> DURATION_LIST = new GenericType<>() {};

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withPlugin(new PostgresPlugin());

    TestJavaTimeSqlArrays() {
        super(PG_ARRAY_TESTS);
    }

    @BeforeAll
    public void setUp() {
        try (var handle = pgExtension.openHandle()) {
            handle.useTransaction(th -> {
                th.execute("drop table if exists time_array_test");
                th.execute("""
                        create table time_array_test (
                        d date[],
                        t time[],
                        ts timestamp[],
                        tstz timestamptz[],
                        ttz timetz[],
                        iv interval[])
                    """);
            });
        }
    }

    @BeforeEach
    public void cleanUp() {
        try (var handle = pgExtension.openHandle()) {
            handle.execute("delete from time_array_test");
        }
    }

    @Override
    protected Handle getHandle() {
        return pgExtension.openHandle();
    }

    private <T> List<T> castRoundTrip(String sqlType, GenericType<List<T>> type, List<T> values) {
        return h.createQuery("SELECT CAST(:v AS " + sqlType + "[])")
                .bindByType("v", values, type)
                .mapTo(type)
                .one();
    }

    private <T> String bindAsText(String sqlType, GenericType<List<T>> type, List<T> values) {
        // the text form of timestamptz depends on the session time zone; pin it so the
        // assertions hold when the suite runs under a different JVM default zone
        h.execute("SET TIME ZONE 'UTC'");
        return h.createQuery("SELECT CAST(CAST(:v AS " + sqlType + "[]) AS text)")
                .bindByType("v", values, type)
                .mapTo(String.class)
                .one();
    }

    @Test
    public void testLocalDateBcAndWideYears() {
        List<LocalDate> dates = List.of(
                LocalDate.of(0, 1, 1),
                LocalDate.of(-500, 6, 15),
                LocalDate.of(10000, 1, 1));
        assertThat(bindAsText("date", LOCAL_DATE_LIST, dates))
                .isEqualTo("{\"0001-01-01 BC\",\"0501-06-15 BC\",10000-01-01}");
        assertThat(castRoundTrip("date", LOCAL_DATE_LIST, dates)).containsExactlyElementsOf(dates);
    }

    @Test
    public void testLocalDateTimeBcAndWideYears() {
        List<LocalDateTime> timestamps = List.of(
                LocalDateTime.of(-500, 6, 15, 10, 15, 30),
                LocalDateTime.of(10000, 1, 1, 0, 0));
        assertThat(bindAsText("timestamp", LOCAL_DATE_TIME_LIST, timestamps))
                .isEqualTo("{\"0501-06-15 10:15:30 BC\",\"10000-01-01 00:00:00\"}");
        assertThat(castRoundTrip("timestamp", LOCAL_DATE_TIME_LIST, timestamps)).containsExactlyElementsOf(timestamps);
    }

    @Test
    public void testOffsetDateTimeBcArray() {
        List<OffsetDateTime> timestamps = List.of(OffsetDateTime.of(-500, 6, 15, 10, 15, 30, 0, ZoneOffset.UTC));
        assertThat(bindAsText("timestamptz", OFFSET_DATE_TIME_LIST, timestamps))
                .isEqualTo("{\"0501-06-15 10:15:30+00 BC\"}");
    }

    @Test
    public void testLocalDateInfinity() {
        List<LocalDate> dates = List.of(LocalDate.MAX, LocalDate.MIN);
        assertThat(bindAsText("date", LOCAL_DATE_LIST, dates)).isEqualTo("{infinity,-infinity}");
        assertThat(castRoundTrip("date", LOCAL_DATE_LIST, dates)).containsExactlyElementsOf(dates);
    }

    @Test
    public void testLocalDateTimeInfinity() {
        List<LocalDateTime> timestamps = List.of(LocalDateTime.MAX, LocalDateTime.MIN);
        assertThat(bindAsText("timestamp", LOCAL_DATE_TIME_LIST, timestamps)).isEqualTo("{infinity,-infinity}");
        assertThat(castRoundTrip("timestamp", LOCAL_DATE_TIME_LIST, timestamps)).containsExactlyElementsOf(timestamps);
    }

    @Test
    public void testOffsetDateTimeInfinity() {
        List<OffsetDateTime> timestamps = List.of(OffsetDateTime.MAX, OffsetDateTime.MIN);
        assertThat(bindAsText("timestamptz", OFFSET_DATE_TIME_LIST, timestamps)).isEqualTo("{infinity,-infinity}");
        assertThat(castRoundTrip("timestamptz", OFFSET_DATE_TIME_LIST, timestamps)).containsExactlyElementsOf(timestamps);
    }

    @Test
    public void testUnnestLocalDateArray() {
        List<LocalDate> dates = List.of(LocalDate.of(2025, 11, 1), LocalDate.of(2025, 11, 2));
        List<LocalDate> unnested = h.createQuery(
                        "SELECT d FROM unnest(CAST(:v AS date[])) AS t(d) ORDER BY d")
                .bindByType("v", dates, LOCAL_DATE_LIST)
                .mapTo(LocalDate.class)
                .list();
        assertThat(unnested).containsExactlyElementsOf(dates);
    }

    /** The shared battery stays at millisecond precision; Postgres keeps microseconds. */
    @Test
    public void testMicrosecondPrecision() {
        List<LocalTime> times = List.of(LocalTime.of(23, 59, 59, 999999000));
        assertThat(castRoundTrip("time", LOCAL_TIME_LIST, times)).containsExactlyElementsOf(times);

        List<LocalDateTime> timestamps = List.of(LocalDateTime.of(2025, 6, 15, 10, 15, 30, 123456000));
        assertThat(castRoundTrip("timestamp", LOCAL_DATE_TIME_LIST, timestamps)).containsExactlyElementsOf(timestamps);

        List<OffsetDateTime> zoned = List.of(OffsetDateTime.of(2025, 6, 15, 10, 15, 30, 123456000, ZoneOffset.ofHoursMinutes(5, 30)));
        assertThat(castRoundTrip("timestamptz", OFFSET_DATE_TIME_LIST, zoned))
                .zipSatisfy(zoned, (actual, expected) -> assertThat(actual.toInstant()).isEqualTo(expected.toInstant()));

        List<Duration> durations = List.of(Duration.ofSeconds(59, 123456000));
        assertThat(castRoundTrip("interval", DURATION_LIST, durations)).containsExactlyElementsOf(durations);
    }

    @Test
    public void testLocalTimeNanosRoundToMicros() {
        // 499ns and 501ns round the same way on every server version; a 500ns tie does not
        List<LocalTime> result = castRoundTrip("time", LOCAL_TIME_LIST,
                List.of(LocalTime.of(10, 15, 30, 123456499), LocalTime.of(10, 15, 30, 123456501)));
        assertThat(result).containsExactly(
                LocalTime.of(10, 15, 30, 123456000), LocalTime.of(10, 15, 30, 123457000));
    }

    @Test
    public void testDurationSubMicrosecondsRoundServerSide() {
        // 1499ns and 1501ns round the same way on every server version; a 1500ns tie does not
        List<Duration> result = castRoundTrip("interval", DURATION_LIST,
                List.of(Duration.ofNanos(1), Duration.ofNanos(1499), Duration.ofNanos(1501)));
        assertThat(result).containsExactly(Duration.ZERO, Duration.ofNanos(1000), Duration.ofNanos(2000));
    }

    @Test
    public void testDurationBeyondServerRangeIsRejected() {
        assertThatThrownBy(() -> castRoundTrip("interval", DURATION_LIST,
                List.of(Duration.ofSeconds(Long.MAX_VALUE))))
                .isInstanceOf(UnableToExecuteStatementException.class);
    }
}

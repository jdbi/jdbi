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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.assertj.core.data.TemporalUnitOffset;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.meta.Legacy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Set of tests for java.time types. Setup a table "time_test" with the following columns:<br>
 * <ul>
 *     <li>ts timestamp</li>
 *     <li>d date</li>
 *     <li>t time</li>
 *     <li>dt datetime</li>
 *     <li>tstz timestamp with timezone</li>
 *     <li>ttz time with timezone</li>
 * </ul>
 * <p>
 * Each column is optional. More columns are possible.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractJavaTimeTests {

    private static final long TWO_DAY_OFFSET = 2 * 24 * 60 * 60 * 1000;

    static final ZoneOffset TEST_ZONE_OFFSET;
    static final ZoneId TEST_ZONE_ID;

    static {
        // Ah Time. Flowing like a river...

        var testZoneOffset = ZoneOffset.of("-02:30");
        if (ZoneId.systemDefault().getRules().getOffset(Instant.now()).equals(testZoneOffset)) {
            testZoneOffset = ZoneOffset.of("-09:30");
        }

        var testZoneId = ZoneId.of("Asia/Katmandu"); // +05:45 ...
        if (ZoneId.systemDefault().getId().equals(testZoneId.getId())) {
            testZoneId = ZoneId.of("Australia/Broken_Hill"); // +10:30 ...
        }

        TEST_ZONE_OFFSET = testZoneOffset;
        TEST_ZONE_ID = testZoneId;
    }

    private final Map<Class<?>, String[]> columnsForTypes;

    protected Handle h;

    @BeforeEach
    public void createHandle() {
        this.h = getHandle();
    }

    @AfterEach
    public void destroyHandle() {
        h.close();
        this.h = null;
    }

    protected abstract Handle getHandle();

    protected AbstractJavaTimeTests(Map<Class<?>, String[]> columnsForTypes) {
        this.columnsForTypes = columnsForTypes;
    }

    /**
     * Override if the type should be a qualified type.
     */
    protected <T> QualifiedType<T> getTestType(Class<T> clazz) {
        return QualifiedType.of(clazz);
    }

    /**
     * Default precision for is a microsecond. Override if that is not true for a specific database or data type.
     */
    protected TemporalUnitOffset getAllowableOffset(TestResult result) {
        return within(0, ChronoUnit.MICROS);
    }

    protected ZoneOffset getExpectedZoneOffset(Class<?> clazz, Object value, String column) {
        if (value instanceof ZonedDateTime zdt) {
            return zdt.getOffset();
        } else if (value instanceof OffsetDateTime odt) {
            return odt.getOffset();
        } else if (value instanceof OffsetTime ot) {
            return ot.getOffset();
        } else {
            return getSystemDefaultOffset();
        }
    }

    /**
     * Helper returning the current offset for the system default zone.
     */
    protected ZoneOffset getSystemDefaultOffset() {
        return ZoneId.systemDefault().getRules().getOffset(NOWISH_INSTANT);
    }

    /**
     * Helper for legacy testing code.
     */
    protected final <T> QualifiedType<T> getLegacyTestType(Class<T> clazz) {
        if (clazz.getPackage().getName().startsWith("java.time")) {
            return QualifiedType.of(clazz).with(Legacy.class);
        } else {
            return QualifiedType.of(clazz);
        }
    }

    /**
     * Helper for legacy testing code.
     */
    protected final TemporalUnitOffset getLegacyAllowableOffset(TestResult result) {
        if (result.type() == OffsetTime.class || result.type() == LocalTime.class) {
            // The legacy wrappers for {@link OffsetTime} and {@link LocalTime} only support second precision.
            return within(0, ChronoUnit.SECONDS);
        } else {
            return within(0, ChronoUnit.MICROS);
        }
    }

    /**
     * Helper for legacy testing code.
     */
    protected final ZoneOffset getLegacyExpectedZoneOffset(Class<?> type) {
        if (type == OffsetTime.class) {
            return ZoneOffset.UTC;
        } else {
            return ZoneId.systemDefault().getRules().getOffset(NOWISH_INSTANT);
        }
    }

    // Argument providers for the actual test.

    static Stream<Class<?>> timeTypes() {
        return Stream.of(OffsetDateTime.class, ZonedDateTime.class, OffsetTime.class,
            Time.class, Date.class, Timestamp.class,
            Instant.class, LocalDate.class, LocalTime.class, LocalDateTime.class);
    }

    // don't use the current date, otherwise datetime -> time column will seem to work because it fakes the local date.
    static final long NOWISH = System.currentTimeMillis() + TWO_DAY_OFFSET;
    static final Instant NOWISH_INSTANT = Instant.ofEpochMilli(NOWISH);

    // null out milliseconds to not run into rounding issues with e.g. MySQL
    static final long NOWISH_NO_MILLIS = NOWISH - (NOWISH % 1000);

    static Map<Class<?>, Supplier<Object>> valueSuppliers() {
        return Map.of(
            OffsetDateTime.class, () -> OffsetDateTime.ofInstant(NOWISH_INSTANT, TEST_ZONE_OFFSET),
            ZonedDateTime.class, () -> ZonedDateTime.ofInstant(NOWISH_INSTANT, TEST_ZONE_ID),
            OffsetTime.class, () -> OffsetTime.ofInstant(NOWISH_INSTANT, TEST_ZONE_OFFSET),
            Time.class, () -> new Time(NOWISH_NO_MILLIS),
            Date.class, () -> new Date(NOWISH),
            Timestamp.class, () -> new Timestamp(NOWISH),
            Instant.class, () -> Instant.ofEpochMilli(NOWISH),
            LocalDate.class, () -> LocalDate.ofInstant(NOWISH_INSTANT, ZoneId.systemDefault()),
            LocalTime.class, () -> LocalTime.ofInstant(NOWISH_INSTANT, ZoneId.systemDefault()),
            LocalDateTime.class, () -> LocalDateTime.ofInstant(NOWISH_INSTANT, ZoneId.systemDefault())
        );
    }

    public record TestResult(Object expected, Object actual, String columnName, Class<?> type, ZoneOffset offset) {}

    protected Map<Class<?>, Consumer<TestResult>> comparators() {
        return Map.of(
            OffsetDateTime.class,
            (result) -> {
                var actual = (OffsetDateTime) result.actual();
                var expected = (OffsetDateTime) result.expected();
                assertThat(actual.getOffset()).isEqualTo(result.offset());
                assertThat(actual).isCloseTo(expected, getAllowableOffset(result));
            },
            ZonedDateTime.class,
            (result) -> {
                var actual = (ZonedDateTime) result.actual();
                var expected = (ZonedDateTime) result.expected();
                assertThat(actual.getOffset()).isEqualTo(result.offset());
                assertThat(actual).isCloseTo(expected, getAllowableOffset(result));
            },
            OffsetTime.class, (result) -> {
                var actual = (OffsetTime) result.actual();
                var expected = (OffsetTime) result.expected();
                assertThat(actual.getOffset()).isEqualTo(result.offset());
                assertThat(actual.toLocalTime()).isCloseTo(expected.toLocalTime(), getAllowableOffset(result));
            },
            Time.class, (result) ->
                assertThat(((Time) result.actual()).toLocalTime()).isCloseTo(((Time) result.expected()).toLocalTime(), getAllowableOffset(result)),
            LocalTime.class, (result) ->
                assertThat((LocalTime) result.actual()).isCloseTo(((LocalTime) result.expected()), getAllowableOffset(result)),
            Date.class, (result) ->
                assertThat(((Date) result.actual()).toLocalDate()).isEqualTo(((Date) result.expected()).toLocalDate()),
            LocalDate.class, (result) ->
                assertThat((LocalDate) result.actual()).isEqualTo(result.expected()),
            Instant.class, (result) ->
                assertThat((Instant) result.actual()).isCloseTo((Instant) result.expected(), getAllowableOffset(result)),
            Timestamp.class, (result) ->
                assertThat(((Timestamp) result.actual()).toInstant()).isCloseTo(((Timestamp) result.expected()).toInstant(), getAllowableOffset(result)),
            LocalDateTime.class,
            (result) ->
                assertThat((LocalDateTime) result.actual()).isCloseTo((LocalDateTime) result.expected(), getAllowableOffset(result))
        );
    }

    Stream<Arguments> argumentsProvider() {
        var valueSupplierForType = valueSuppliers();
        var comparatorForType = comparators();

        List<Arguments> arguments = new ArrayList<>();
        timeTypes().forEach(type -> {
            var columns = columnsForTypes.get(type);
            if (columns != null) {
                Stream.of(columns).forEach(column -> arguments.add(
                    Arguments.of(type, column, requireNonNull(valueSupplierForType.get(type)), requireNonNull(comparatorForType.get(type)))));
            }
        });
        return arguments.stream();
    }

    @DisplayName("Test java.time types roundtrip")
    @ParameterizedTest(name = "[{index}]: type = {0}, column = {1}")
    @MethodSource("argumentsProvider")
    public void testJavaTimeType(Class<?> clazz, String column, Supplier<Object> valueSupplier, Consumer<TestResult> comparator) {
        var type = getTestType(clazz);
        var value = valueSupplier.get();
        var expectedZoneOffset = getExpectedZoneOffset(clazz, value, column);

        try (Update u = h.createUpdate(format("insert into time_test(%s) values (?)", column))) {
            u.bindByType(0, value, type);
            u.execute();
        }

        var result = h.createQuery(format("select %s from time_test", column)).mapTo(type).one();

        comparator.accept(new TestResult(value, result, column, clazz, expectedZoneOffset));
    }

    @DisplayName("Test java.time types null value")
    @ParameterizedTest(name = "[{index}]: type = {0}, column = {1}")
    @MethodSource("argumentsProvider")
    public void testJavaTimeTypeNull(Class<?> clazz, String column) {
        var type = getTestType(clazz);

        try (Update u = h.createUpdate(format("insert into time_test(%s) values (?)", column))) {
            u.bindByType(0, null, type);
            u.execute();
        }

        var result = h.createQuery(format("select %s from time_test", column)).mapTo(type).one();

        assertThat(result).isNull();
    }
}

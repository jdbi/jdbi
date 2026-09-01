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
package org.jdbi.v3.core.array;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.statement.Update;
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

/**
 * Round-trip battery for the {@code java.time} SQL array element types registered by default in
 * {@link SqlArrayTypes} and by database plugins.
 * <p>
 * A database-specific test class extends this class, creates a table named
 * {@code time_array_test} with array columns in its own dialect, and passes a map from element
 * type to column name; types absent from the map are not tested. The test values stay within
 * years 1 through 9999 and millisecond precision, so they are valid on any array-capable
 * backend; database-specific edge cases and precision guarantees belong in the subclass.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractJavaTimeArrayTests {

    private final Map<Class<?>, String> columnsForTypes;

    protected Handle h;

    protected AbstractJavaTimeArrayTests(Map<Class<?>, String> columnsForTypes) {
        this.columnsForTypes = columnsForTypes;
    }

    protected abstract Handle getHandle();

    @BeforeEach
    public void createHandle() {
        this.h = getHandle();
    }

    @AfterEach
    public void destroyHandle() {
        h.close();
        this.h = null;
    }

    static Stream<Class<?>> elementTypes() {
        return Stream.of(LocalDate.class, LocalTime.class, LocalDateTime.class, OffsetDateTime.class,
            OffsetTime.class, Instant.class, ZonedDateTime.class, Duration.class, Period.class);
    }

    // millisecond precision only: not every backend keeps microseconds in array elements
    // (H2 truncates TIME array elements to milliseconds); precision is a per-database test
    static Map<Class<?>, List<?>> valuesForTypes() {
        return Map.of(
            LocalDate.class, List.of(
                LocalDate.of(2025, 6, 15),
                LocalDate.of(2024, 2, 29),
                LocalDate.of(1, 1, 1),
                LocalDate.of(9999, 12, 31)),
            LocalTime.class, List.of(
                LocalTime.MIDNIGHT,
                LocalTime.of(10, 15, 30),
                LocalTime.of(23, 59, 59, 999000000)),
            LocalDateTime.class, List.of(
                LocalDateTime.of(2025, 6, 15, 10, 15, 30, 123000000),
                LocalDateTime.of(1, 1, 1, 0, 0),
                LocalDateTime.of(9999, 12, 31, 23, 59, 59)),
            OffsetDateTime.class, List.of(
                OffsetDateTime.of(2025, 6, 15, 10, 15, 30, 123000000, ZoneOffset.ofHoursMinutes(5, 30)),
                OffsetDateTime.of(2025, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-8)),
                OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)),
            OffsetTime.class, List.of(
                OffsetTime.of(10, 15, 30, 0, ZoneOffset.ofHours(-8)),
                OffsetTime.of(23, 59, 59, 999000000, ZoneOffset.ofHoursMinutes(5, 30)),
                OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC)),
            Instant.class, List.of(
                Instant.EPOCH,
                Instant.parse("2025-06-15T10:15:30.123Z"),
                Instant.parse("1583-01-01T00:00:00Z"),
                Instant.parse("9999-12-31T23:59:59Z")),
            ZonedDateTime.class, List.of(
                ZonedDateTime.of(2025, 6, 15, 10, 15, 30, 0, ZoneId.of("Europe/Paris")),
                ZonedDateTime.of(2025, 1, 15, 10, 15, 30, 0, ZoneId.of("America/Los_Angeles"))),
            Duration.class, List.of(
                Duration.ZERO,
                Duration.ofHours(2).plusMinutes(30),
                Duration.ofHours(-2).plusMinutes(-30),
                Duration.ofDays(40),
                Duration.ofSeconds(59, 123000000)),
            Period.class, List.of(
                Period.ZERO,
                Period.of(1, 2, 3),
                Period.of(-1, -2, -3),
                Period.ofDays(400)));
    }

    /**
     * Instant-carrying types come back in the database's rendering zone, so the instant is
     * compared; everything else round-trips exactly. Override for a database that differs.
     */
    protected BiConsumer<Object, Object> elementComparatorFor(Class<?> clazz) {
        if (clazz == OffsetDateTime.class) {
            return (expected, actual) ->
                assertThat(((OffsetDateTime) actual).toInstant()).isEqualTo(((OffsetDateTime) expected).toInstant());
        }
        if (clazz == ZonedDateTime.class) {
            return (expected, actual) ->
                assertThat(((ZonedDateTime) actual).toInstant()).isEqualTo(((ZonedDateTime) expected).toInstant());
        }
        return (expected, actual) -> assertThat(actual).isEqualTo(expected);
    }

    Stream<Arguments> argumentsProvider() {
        var valuesForTypes = valuesForTypes();

        List<Arguments> arguments = new ArrayList<>();
        elementTypes().forEach(type -> {
            var column = columnsForTypes.get(type);
            if (column != null) {
                arguments.add(Arguments.of(type, column, requireNonNull(valuesForTypes.get(type))));
            }
        });
        return arguments.stream();
    }

    @DisplayName("Test java.time array round trip")
    @ParameterizedTest(name = "[{index}]: type = {0}, column = {1}")
    @MethodSource("argumentsProvider")
    public void testArrayRoundTrip(Class<?> clazz, String column, List<?> values) {
        compare(clazz, values, roundTrip(clazz, column, values));
    }

    @DisplayName("Test java.time array with null element")
    @ParameterizedTest(name = "[{index}]: type = {0}, column = {1}")
    @MethodSource("argumentsProvider")
    public void testArrayNullElement(Class<?> clazz, String column, List<?> values) {
        var withNull = Arrays.asList(values.get(0), null);
        compare(clazz, withNull, roundTrip(clazz, column, withNull));
    }

    @DisplayName("Test empty java.time array")
    @ParameterizedTest(name = "[{index}]: type = {0}, column = {1}")
    @MethodSource("argumentsProvider")
    public void testEmptyArray(Class<?> clazz, String column, List<?> values) {
        assertThat(roundTrip(clazz, column, List.of())).isEmpty();
    }

    private List<?> roundTrip(Class<?> clazz, String column, List<?> values) {
        Type listType = GenericTypes.parameterizeClass(List.class, clazz);

        try (Update u = h.createUpdate(format("insert into time_array_test (%s) values (?)", column))) {
            u.bindByType(0, values, listType);
            u.execute();
        }

        return (List<?>) h.createQuery(format("select %s from time_array_test", column)).mapTo(listType).one();
    }

    private void compare(Class<?> clazz, List<?> expected, List<?> actual) {
        assertThat(actual).hasSameSizeAs(expected);

        var comparator = elementComparatorFor(clazz);
        for (int i = 0; i < expected.size(); i++) {
            if (expected.get(i) == null) {
                assertThat(actual.get(i)).isNull();
            } else {
                comparator.accept(expected.get(i), actual.get(i));
            }
        }
    }
}

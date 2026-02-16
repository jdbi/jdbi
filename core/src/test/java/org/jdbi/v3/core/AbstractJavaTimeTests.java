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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.assertj.core.data.TemporalUnitOffset;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.Update;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public abstract class AbstractJavaTimeTests {

    protected Handle h;

    protected static List<String> findZoneIdsFor(ZonedDateTime dt) {
        var offset = dt.getOffset();
        return ZoneId.getAvailableZoneIds().stream()
            .filter(id -> ZoneId.of(id).getRules().getOffset(dt.toInstant()).equals(offset))
            .sorted()
            .toList();
    }

    protected <T> QualifiedType<T> getTestType(Class<T> clazz) {
        return QualifiedType.of(clazz);
    }

    protected TemporalUnitOffset getAllowableOffset() {
        return within(0, ChronoUnit.MICROS);
    }

    protected boolean skipInstant() {
        return false;
    }

    protected boolean skipLocalTime() {
        return false;
    }

    protected boolean skipObjectTimestamp() {
        return false;
    }

    protected boolean skipTSTZ() {
        return false;
    }

    public final boolean skipInstantOrTSTZ() {
        return skipInstant() || skipTSTZ();
    }

    public final boolean skipObjectTimestampOrTSTZ() {
        return skipObjectTimestamp() || skipTSTZ();
    }

    @Test
    @DisabledIf("skipInstant")
    public final void instant() {
        var type = getTestType(Instant.class);
        var expected = Instant.now();

        try (Update u = h.createUpdate("insert into stuff(ts) values (?)")) {
            u.bindByType(0, expected, type);
            u.execute();
        }

        var result = h.createQuery("select ts from stuff").mapTo(type).one();

        validateInstant(result, expected);
    }

    protected void validateInstant(Instant result, Instant expected) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());
    }

    @Test
    @DisabledIf("skipInstantOrTSTZ")
    public final void instantTSTZ() {
        var type = getTestType(Instant.class);
        var expected = Instant.now();

        try (Update u = h.createUpdate("insert into stuff(tstz) values (?)")) {
            u.bindByType(0, expected, type);
            u.execute();
        }

        var result = h.createQuery("select tstz from stuff").mapTo(type).one();

        validateInstantTSTZ(result, expected);
    }

    protected void validateInstantTSTZ(Instant result, Instant expected) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());
    }

    @Test
    @DisabledIf("skipInstant")
    public final void instantNull() {
        var type = getTestType(Instant.class);

        try (Update u = h.createUpdate("insert into stuff(ts) values (?)")) {
            u.bindByType(0, null, type);
            u.execute();
        }

        var result = h.createQuery("select ts from stuff").mapTo(type).one();

        assertThat(result).isNull();
    }

    @Test
    @DisabledIf("skipInstantOrTSTZ")
    public final void instantNullTSTZ() {
        var type = getTestType(Instant.class);

        try (Update u = h.createUpdate("insert into stuff(tstz) values (?)")) {
            u.bindByType(0, null, type);
            u.execute();
        }

        var result = h.createQuery("select tstz from stuff").mapTo(type).one();

        assertThat(result).isNull();
    }

    @Test
    public final void localDate() {
        var type = getTestType(LocalDate.class);
        LocalDate expected = LocalDate.now(ZoneId.systemDefault());

        try (Update u = h.createUpdate("insert into stuff(d) values (?)")) {
            u.bindByType(0, expected, type);
            u.execute();
        }

        var result = h.createQuery("select d from stuff").mapTo(type).one();

        validateLocalDate(result, expected);
    }

    protected void validateLocalDate(LocalDate result, LocalDate expected) {
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public final void localDateNull() {
        var type = getTestType(LocalDate.class);

        try (Update u = h.createUpdate("insert into stuff(d) values (?)")) {
            u.bindByType(0, null, type);
            u.execute();
        }

        var result = h.createQuery("select d from stuff").mapTo(type).one();

        assertThat(result).isNull();
    }

    @Test
    @DisabledIf("skipLocalTime")
    public final void localTime() {
        var type = getTestType(LocalTime.class);
        var expected = LocalTime.now(ZoneId.systemDefault());

        try (Update u = h.createUpdate("insert into stuff(t) values (?)")) {
            u.bindByType(0, expected, type);
            u.execute();
        }

        var result = h.createQuery("select t from stuff").mapTo(type).one();

        validateLocalTime(result, expected);
    }

    protected void validateLocalTime(LocalTime result, LocalTime expected) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());
    }

    @Test
    @DisabledIf("skipLocalTime")
    public final void localTimeNull() {
        var type = getTestType(LocalTime.class);

        try (Update u = h.createUpdate("insert into stuff(t) values (?)")) {
            u.bindByType(0, null, type);
            u.execute();
        }

        var result = h.createQuery("select t from stuff").mapTo(type).one();

        assertThat(result).isNull();
    }

    @Test
    public final void localDateTime() {
        var type = getTestType(LocalDateTime.class);
        LocalDateTime expected = LocalDateTime.now(ZoneId.systemDefault());

        try (Update u = h.createUpdate("insert into stuff(ts) values (?)")) {
            u.bindByType(0, expected, type);
            u.execute();
        }

        var result = h.createQuery("select ts from stuff").mapTo(type).one();

        validateLocalDateTime(result, expected);
    }

    protected void validateLocalDateTime(LocalDateTime result, LocalDateTime expected) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());
    }

    @Test
    public final void localDateTimeNull() {
        var type = getTestType(LocalDateTime.class);

        try (Update u = h.createUpdate("insert into stuff(ts) values (?)")) {
            u.bindByType(0, null, type);
            u.execute();
        }

        var result = h.createQuery("select ts from stuff").mapTo(type).one();

        assertThat(result).isNull();
    }

    @Test
    @DisabledIf("skipObjectTimestamp")
    public final void offsetDateTimeLosesOffsetWithTimestamp() {
        var type = getTestType(OffsetDateTime.class);
        var now = Instant.now();

        var defaultOffset = ZoneOffset.systemDefault().getRules().getOffset(now);
        var testOffset = ZoneOffset.ofHoursMinutes(-7, -15);

        // use a non-existing offset to ensure tests don't fail because local time happens to be this offset.
        OffsetDateTime expected = now.atOffset(testOffset);

        assertThat(expected.getOffset()).isNotEqualTo(defaultOffset);
        assertThat(expected.getOffset()).isEqualTo(testOffset);

        try (Update u = h.createUpdate("insert into stuff(ts) values (?)")) {
            u.bindByType(0, expected, type);
            u.execute();
        }

        var result = h.createQuery("select ts from stuff").mapTo(type).one();

        validateOffsetDateTimeLosesOffsetWithTimestamp(result, expected, defaultOffset, testOffset);
    }

    protected void validateOffsetDateTimeLosesOffsetWithTimestamp(OffsetDateTime result, OffsetDateTime expected, ZoneOffset defaultOffset,
        ZoneOffset testOffset) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());
        // falls back to default offset
        assertThat(result.getOffset()).isEqualTo(defaultOffset);
        assertThat(result.getOffset()).isNotEqualTo(testOffset);
    }

    @Test
    @DisabledIf("skipObjectTimestamp")
    public final void offsetDateTimeNull() {
        var type = getTestType(OffsetDateTime.class);

        try (Update u = h.createUpdate("insert into stuff(ts) values (?)")) {
            u.bindByType(0, null, type);
            u.execute();
        }

        var result = h.createQuery("select ts from stuff").mapTo(type).one();

        assertThat(result).isNull();
    }

    @Test
    @DisabledIf("skipObjectTimestampOrTSTZ")
    public final void offsetDateTimeNullTSTZ() {
        var type = getTestType(OffsetDateTime.class);

        try (Update u = h.createUpdate("insert into stuff(tstz) values (?)")) {
            u.bindByType(0, null, type);
            u.execute();
        }

        var result = h.createQuery("select tstz from stuff").mapTo(type).one();

        assertThat(result).isNull();
    }

    @Test
    @DisabledIf("skipObjectTimestamp")
    public final void zonedDateTimeLosesZoneWithTimestamp() {
        var now = ZonedDateTime.now(ZoneId.systemDefault());
        var type = getTestType(ZonedDateTime.class);

        var defaultZoneId = ZoneId.systemDefault();
        var testZoneId = ZoneId.of("America/Denver");
        if (defaultZoneId.equals(testZoneId)) {
            testZoneId = ZoneId.of("America/Los_Angeles");
        }

        ZonedDateTime expected = now.withZoneSameInstant(testZoneId);

        assertThat(expected.getZone()).isNotEqualTo(defaultZoneId);
        assertThat(expected.getZone()).isEqualTo(testZoneId);

        try (Update u = h.createUpdate("insert into stuff(ts) values (?)")) {
            u.bindByType(0, expected, type);
            u.execute();
        }

        var result = h.createQuery("select ts from stuff").mapTo(type).one();

        validateZonedDateTimeLosesZoneWithTimestamp(result, expected, defaultZoneId, testZoneId);
    }

    /**
     * by default, the driver should map onto the default timezone, not the test timezone.
     */
    protected void validateZonedDateTimeLosesZoneWithTimestamp(ZonedDateTime result, ZonedDateTime expected, ZoneId defaultZoneId, ZoneId testZoneId) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());

        var matchingZoneIds = findZoneIdsFor(result);

        assertThat(matchingZoneIds).contains(defaultZoneId.getId());
        assertThat(matchingZoneIds).doesNotContain(testZoneId.getId());
    }

    @Test
    @DisabledIf("skipTSTZ")
    public final void offsetDateTimeTSTZ() {
        var type = getTestType(OffsetDateTime.class);

        var defaultOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        var testOffset = ZoneOffset.ofHoursMinutes(-7, -15);

        // use a non-existing offset to ensure tests don't fail because local time happens to be this offset.
        OffsetDateTime expected = OffsetDateTime.now(ZoneId.systemDefault()).withOffsetSameInstant(testOffset);

        assertThat(expected.getOffset()).isNotEqualTo(defaultOffset);
        assertThat(expected.getOffset()).isEqualTo(testOffset);

        try (Update u = h.createUpdate("insert into stuff(tstz) values (?)")) {
            u.bindByType(0, expected, type);
            u.execute();
        }

        var result = h.createQuery("select tstz from stuff").mapTo(type).one();
        validateOffsetDateTimeTSTZ(result, expected, defaultOffset, testOffset);
    }

    protected void validateOffsetDateTimeTSTZ(OffsetDateTime result, OffsetDateTime expected, ZoneOffset defaultOffset, ZoneOffset testOffset) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());

        // preserved through the roundtrip
        assertThat(result.getOffset()).isNotEqualTo(defaultOffset);
        assertThat(result.getOffset()).isEqualTo(testOffset);
    }

    @Test
    @DisabledIf("skipInstantOrTSTZ")
    public void zonedDateTimeTSTZ() {
        var type = getTestType(ZonedDateTime.class);

        var defaultZoneId = ZoneId.systemDefault();
        var testZoneId = ZoneId.of("America/Denver");
        if (defaultZoneId.equals(testZoneId)) {
            testZoneId = ZoneId.of("America/Los_Angeles");
        }

        ZonedDateTime expected = ZonedDateTime.now(ZoneId.systemDefault()).withZoneSameInstant(testZoneId);

        assertThat(expected.getZone()).isNotEqualTo(defaultZoneId);
        assertThat(expected.getZone()).isEqualTo(testZoneId);

        try (Update u = h.createUpdate("insert into stuff(tstz) values (?)")) {
            u.bindByType(0, expected, type);
            u.execute();
        }

        var result = h.createQuery("select tstz from stuff").mapTo(type).one();

        validateZonedDateTimeTSTZ(result, expected, defaultZoneId, testZoneId);
    }

    /**
     * preserved through the roundtrip. Test is a bit convoluted because the result only contains an offset because Offset may map to multiple zones.
     */
    protected void validateZonedDateTimeTSTZ(ZonedDateTime result, ZonedDateTime expected, ZoneId defaultZoneId, ZoneId testZoneId) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());

        List<String> matchingZoneIds = findZoneIdsFor(result);
        assertThat(matchingZoneIds).contains(testZoneId.getId());
        assertThat(matchingZoneIds).doesNotContain(defaultZoneId.getId());
    }

    @Test
    @DisabledIf("skipObjectTimestamp")
    public final void zonedDateTimeNull() {
        var type = getTestType(ZonedDateTime.class);

        try (Update u = h.createUpdate("insert into stuff(ts) values (?)")) {
            u.bindByType(0, null, type);
            u.execute();
        }

        var result = h.createQuery("select ts from stuff").mapTo(type).one();

        assertThat(result).isNull();
    }

    @Test
    @DisabledIf("skipObjectTimestampOrTSTZ")
    public final void zonedDateTimeNullTSTZ() {
        var type = getTestType(ZonedDateTime.class);

        try (Update u = h.createUpdate("insert into stuff(tstz) values (?)")) {
            u.bindByType(0, null, type);
            u.execute();
        }

        var result = h.createQuery("select tstz from stuff").mapTo(type).one();

        assertThat(result).isNull();
    }

    @Test
    public final void zoneId() {
        final ZoneId expected = ZoneId.systemDefault();
        h.execute("insert into stuff(z) values (?)", expected);

        var result = h.createQuery("select z from stuff").mapTo(ZoneId.class).one();

        validateZoneId(result, expected);
    }

    protected void validateZoneId(ZoneId result, ZoneId expected) {
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public final void zoneIdNull() {
        try (Update u = h.createUpdate("insert into stuff(z) values (?)")) {
            u.bindByType(0, null, ZoneId.class);
            u.execute();
        }

        var result = h.createQuery("select z from stuff").mapTo(ZoneId.class).one();

        assertThat(result).isNull();
    }
}

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
package org.jdbi.v3.sqlobject;

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
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.meta.Legacy;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Abstract base test for Java time type mapping via the SqlObject API. Mirrors {@code AbstractJavaTimeTests} from the core module.
 * <p>
 * Subclasses must provide {@link #dao} before tests run (typically in {@code @BeforeEach}), and must create the {@code stuff} table with the following
 * columns:
 * <ul>
 *   <li>{@code ts} - TIMESTAMP</li>
 *   <li>{@code tstz} - TIMESTAMP WITH TIME ZONE</li>
 *   <li>{@code d} - DATE</li>
 *   <li>{@code t} - TIME</li>
 *   <li>{@code z} - TEXT</li>
 * </ul>
 */
public abstract class AbstractSqlObjectJavaTimeTests {

    protected Handle handle;
    protected TimeDao dao;

    protected static List<String> findZoneIdsFor(ZonedDateTime dt) {
        var offset = dt.getOffset();
        return ZoneId.getAvailableZoneIds().stream()
            .filter(id -> ZoneId.of(id).getRules().getOffset(dt.toInstant()).equals(offset))
            .sorted()
            .toList();
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
        var expected = Instant.now();
        dao.insertInstantTs(expected);
        var result = dao.selectInstantTs();

        validateInstant(result, expected);
    }

    protected void validateInstant(Instant result, Instant expected) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());
    }

    @Test
    @DisabledIf("skipInstantOrTSTZ")
    public final void instantTSTZ() {
        var expected = Instant.now();
        dao.insertInstantTstz(expected);
        var result = dao.selectInstantTstz();

        validateInstantTSTZ(result, expected);
    }

    protected void validateInstantTSTZ(Instant result, Instant expected) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());
    }

    @Test
    @DisabledIf("skipInstant")
    public final void instantNull() {
        dao.insertInstantTs(null);
        var result = dao.selectInstantTs();

        assertThat(result).isNull();
    }

    @Test
    @DisabledIf("skipInstantOrTSTZ")
    public final void instantNullTSTZ() {
        dao.insertInstantTstz(null);
        var result = dao.selectInstantTstz();

        assertThat(result).isNull();
    }

    @Test
    public final void localDate() {
        LocalDate expected = LocalDate.now(ZoneId.systemDefault());
        dao.insertLocalDate(expected);
        var result = dao.selectLocalDate();

        validateLocalDate(result, expected);
    }

    protected void validateLocalDate(LocalDate result, LocalDate expected) {
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public final void localDateNull() {
        dao.insertLocalDate(null);
        var result = dao.selectLocalDate();

        assertThat(result).isNull();
    }

    @Test
    @DisabledIf("skipLocalTime")
    public final void localTime() {
        var expected = LocalTime.now(ZoneId.systemDefault());
        dao.insertLocalTime(expected);
        var result = dao.selectLocalTime();

        validateLocalTime(result, expected);
    }

    protected void validateLocalTime(LocalTime result, LocalTime expected) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());
    }

    @Test
    @DisabledIf("skipLocalTime")
    public final void localTimeNull() {
        dao.insertLocalTime(null);
        var result = dao.selectLocalTime();

        assertThat(result).isNull();
    }

    @Test
    public final void localDateTime() {
        LocalDateTime expected = LocalDateTime.now(ZoneId.systemDefault());
        dao.insertLocalDateTimeTs(expected);
        var result = dao.selectLocalDateTimeTs();

        validateLocalDateTime(result, expected);
    }

    protected void validateLocalDateTime(LocalDateTime result, LocalDateTime expected) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());
    }

    @Test
    public final void localDateTimeNull() {
        dao.insertLocalDateTimeTs(null);
        var result = dao.selectLocalDateTimeTs();
        assertThat(result).isNull();
    }

    @Test
    @DisabledIf("skipObjectTimestamp")
    public final void offsetDateTimeLosesOffsetWithTimestamp() {
        var now = Instant.now();
        var defaultOffset = ZoneOffset.systemDefault().getRules().getOffset(now);
        var testOffset = ZoneOffset.ofHoursMinutes(-7, -15);

        OffsetDateTime expected = now.atOffset(testOffset);

        assertThat(expected.getOffset()).isNotEqualTo(defaultOffset);
        assertThat(expected.getOffset()).isEqualTo(testOffset);

        dao.insertOffsetDateTimeTs(expected);
        var result = dao.selectOffsetDateTimeTs();

        validateOffsetDateTimeLosesOffsetWithTimestamp(result, expected, defaultOffset, testOffset);
    }

    protected void validateOffsetDateTimeLosesOffsetWithTimestamp(OffsetDateTime result, OffsetDateTime expected, ZoneOffset defaultOffset,
        ZoneOffset testOffset) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());
        assertThat(result.getOffset()).isEqualTo(defaultOffset);
        assertThat(result.getOffset()).isNotEqualTo(testOffset);
    }

    @Test
    @DisabledIf("skipObjectTimestamp")
    public final void offsetDateTimeNull() {
        dao.insertOffsetDateTimeTs(null);
        var result = dao.selectOffsetDateTimeTs();

        assertThat(result).isNull();
    }

    @Test
    @DisabledIf("skipObjectTimestampOrTSTZ")
    public final void offsetDateTimeNullTSTZ() {
        dao.insertOffsetDateTimeTstz(null);
        var result = dao.selectOffsetDateTimeTstz();

        assertThat(result).isNull();
    }

    @Test
    @DisabledIf("skipObjectTimestamp")
    public final void zonedDateTimeLosesZoneWithTimestamp() {
        var now = ZonedDateTime.now(ZoneId.systemDefault());
        var defaultZoneId = ZoneId.systemDefault();
        var testZoneId = ZoneId.of("America/Denver");
        if (defaultZoneId.equals(testZoneId)) {
            testZoneId = ZoneId.of("America/Los_Angeles");
        }

        ZonedDateTime expected = now.withZoneSameInstant(testZoneId);

        assertThat(expected.getZone()).isNotEqualTo(defaultZoneId);
        assertThat(expected.getZone()).isEqualTo(testZoneId);

        dao.insertZonedDateTimeTs(expected);
        var result = dao.selectZonedDateTimeTs();

        validateZonedDateTimeLosesZoneWithTimestamp(result, expected, defaultZoneId, testZoneId);
    }

    protected void validateZonedDateTimeLosesZoneWithTimestamp(ZonedDateTime result, ZonedDateTime expected, ZoneId defaultZoneId, ZoneId testZoneId) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());

        var matchingZoneIds = findZoneIdsFor(result);
        assertThat(matchingZoneIds).contains(defaultZoneId.getId());
        assertThat(matchingZoneIds).doesNotContain(testZoneId.getId());
    }

    @Test
    @DisabledIf("skipTSTZ")
    public final void offsetDateTimeTSTZ() {
        var defaultOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        var testOffset = ZoneOffset.ofHoursMinutes(-7, -15);

        OffsetDateTime expected = OffsetDateTime.now(ZoneId.systemDefault()).withOffsetSameInstant(testOffset);

        assertThat(expected.getOffset()).isNotEqualTo(defaultOffset);
        assertThat(expected.getOffset()).isEqualTo(testOffset);

        dao.insertOffsetDateTimeTstz(expected);
        var result = dao.selectOffsetDateTimeTstz();

        validateOffsetDateTimeTSTZ(result, expected, defaultOffset, testOffset);
    }

    protected void validateOffsetDateTimeTSTZ(OffsetDateTime result, OffsetDateTime expected, ZoneOffset defaultOffset, ZoneOffset testOffset) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());
        assertThat(result.getOffset()).isNotEqualTo(defaultOffset);
        assertThat(result.getOffset()).isEqualTo(testOffset);
    }

    @Test
    @DisabledIf("skipTSTZ")
    public void zonedDateTimeTSTZ() {
        var defaultZoneId = ZoneId.systemDefault();
        var testZoneId = ZoneId.of("America/Denver");
        if (defaultZoneId.equals(testZoneId)) {
            testZoneId = ZoneId.of("America/Los_Angeles");
        }

        ZonedDateTime expected = ZonedDateTime.now(ZoneId.systemDefault()).withZoneSameInstant(testZoneId);

        assertThat(expected.getZone()).isNotEqualTo(defaultZoneId);
        assertThat(expected.getZone()).isEqualTo(testZoneId);

        dao.insertZonedDateTimeTstz(expected);
        var result = dao.selectZonedDateTimeTstz();

        validateZonedDateTimeTSTZ(result, expected, defaultZoneId, testZoneId);
    }

    protected void validateZonedDateTimeTSTZ(ZonedDateTime result, ZonedDateTime expected, ZoneId defaultZoneId, ZoneId testZoneId) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());

        List<String> matchingZoneIds = findZoneIdsFor(result);
        assertThat(matchingZoneIds).contains(testZoneId.getId());
        assertThat(matchingZoneIds).doesNotContain(defaultZoneId.getId());
    }

    @Test
    @DisabledIf("skipObjectTimestamp")
    public final void zonedDateTimeNull() {
        dao.insertZonedDateTimeTs(null);
        var result = dao.selectZonedDateTimeTs();

        assertThat(result).isNull();
    }

    @Test
    @DisabledIf("skipObjectTimestampOrTSTZ")
    public final void zonedDateTimeNullTSTZ() {
        dao.insertZonedDateTimeTstz(null);
        var result = dao.selectZonedDateTimeTstz();

        assertThat(result).isNull();
    }

    @Test
    public final void zoneId() {
        final ZoneId expected = ZoneId.systemDefault();
        dao.insertZoneId(expected);
        var result = dao.selectZoneId();

        validateZoneId(result, expected);
    }

    protected void validateZoneId(ZoneId result, ZoneId expected) {
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public final void zoneIdNull() {
        dao.insertZoneId(null);
        var result = dao.selectZoneId();

        assertThat(result).isNull();
    }

    public interface TimeDao {
        @SqlUpdate("insert into stuff(ts) values (:value)")
        void insertInstantTs(@Bind("value") Instant value);

        @SqlQuery("select ts from stuff")
        Instant selectInstantTs();

        @SqlUpdate("insert into stuff(tstz) values (:value)")
        void insertInstantTstz(@Bind("value") Instant value);

        @SqlQuery("select tstz from stuff")
        Instant selectInstantTstz();

        @SqlUpdate("insert into stuff(d) values (:value)")
        void insertLocalDate(@Bind("value") LocalDate value);

        @SqlQuery("select d from stuff")
        LocalDate selectLocalDate();

        @SqlUpdate("insert into stuff(t) values (:value)")
        void insertLocalTime(@Bind("value") LocalTime value);

        @SqlQuery("select t from stuff")
        LocalTime selectLocalTime();

        @SqlUpdate("insert into stuff(ts) values (:value)")
        void insertLocalDateTimeTs(@Bind("value") LocalDateTime value);

        @SqlQuery("select ts from stuff")
        LocalDateTime selectLocalDateTimeTs();

        @SqlUpdate("insert into stuff(ts) values (:value)")
        void insertOffsetDateTimeTs(@Bind("value") OffsetDateTime value);

        @SqlQuery("select ts from stuff")
        OffsetDateTime selectOffsetDateTimeTs();

        @SqlUpdate("insert into stuff(tstz) values (:value)")
        void insertOffsetDateTimeTstz(@Bind("value") OffsetDateTime value);

        @SqlQuery("select tstz from stuff")
        OffsetDateTime selectOffsetDateTimeTstz();

        @SqlUpdate("insert into stuff(ts) values (:value)")
        void insertZonedDateTimeTs(@Bind("value") ZonedDateTime value);

        @SqlQuery("select ts from stuff")
        ZonedDateTime selectZonedDateTimeTs();

        @SqlUpdate("insert into stuff(tstz) values (:value)")
        void insertZonedDateTimeTstz(@Bind("value") ZonedDateTime value);

        @SqlQuery("select tstz from stuff")
        ZonedDateTime selectZonedDateTimeTstz();

        @SqlUpdate("insert into stuff(z) values (:value)")
        void insertZoneId(@Bind("value") ZoneId value);

        @SqlQuery("select z from stuff")
        ZoneId selectZoneId();
    }

    public interface LegacyTimeDao extends TimeDao {
        @SqlUpdate("insert into stuff(ts) values (:value)")
        void insertInstantTs(@Legacy @Bind("value") Instant value);

        @SqlQuery("select ts from stuff")
        @Legacy
        Instant selectInstantTs();

        @SqlUpdate("insert into stuff(tstz) values (:value)")
        void insertInstantTstz(@Legacy @Bind("value") Instant value);

        @SqlQuery("select tstz from stuff")
        @Legacy
        Instant selectInstantTstz();

        @SqlUpdate("insert into stuff(d) values (:value)")
        void insertLocalDate(@Legacy @Bind("value") LocalDate value);

        @SqlQuery("select d from stuff")
        @Legacy
        LocalDate selectLocalDate();

        @SqlUpdate("insert into stuff(t) values (:value)")
        void insertLocalTime(@Legacy @Bind("value") LocalTime value);

        @SqlQuery("select t from stuff")
        @Legacy
        LocalTime selectLocalTime();

        @SqlUpdate("insert into stuff(ts) values (:value)")
        void insertLocalDateTimeTs(@Legacy @Bind("value") LocalDateTime value);

        @SqlQuery("select ts from stuff")
        @Legacy
        LocalDateTime selectLocalDateTimeTs();

        @SqlUpdate("insert into stuff(ts) values (:value)")
        void insertOffsetDateTimeTs(@Legacy @Bind("value") OffsetDateTime value);

        @SqlQuery("select ts from stuff")
        @Legacy
        OffsetDateTime selectOffsetDateTimeTs();

        @SqlUpdate("insert into stuff(tstz) values (:value)")
        void insertOffsetDateTimeTstz(@Legacy @Bind("value") OffsetDateTime value);

        @SqlQuery("select tstz from stuff")
        @Legacy
        OffsetDateTime selectOffsetDateTimeTstz();

        @SqlUpdate("insert into stuff(ts) values (:value)")
        void insertZonedDateTimeTs(@Legacy @Bind("value") ZonedDateTime value);

        @SqlQuery("select ts from stuff")
        @Legacy
        ZonedDateTime selectZonedDateTimeTs();

        @SqlUpdate("insert into stuff(tstz) values (:value)")
        void insertZonedDateTimeTstz(@Legacy @Bind("value") ZonedDateTime value);

        @SqlQuery("select tstz from stuff")
        @Legacy
        ZonedDateTime selectZonedDateTimeTstz();
    }
}

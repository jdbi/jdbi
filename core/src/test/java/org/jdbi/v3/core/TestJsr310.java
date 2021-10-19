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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestJsr310 {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(h -> h.execute("create table stuff (ts timestamp, d date)"));

    // Don't use nanoseconds - they'll get truncated off
    Clock fixed = Clock.fixed(Instant.ofEpochSecond(123456789), ZoneOffset.UTC);

    @Test
    public void instant() {
        Handle h = h2Extension.getSharedHandle();

        Instant i = Instant.now(fixed);
        h.execute("insert into stuff(ts) values (?)", i);
        assertThat(h.createQuery("select ts from stuff").mapTo(Instant.class).one()).isEqualTo(i);
    }

    @Test
    public void localDate() {
        Handle h = h2Extension.getSharedHandle();

        LocalDate d = LocalDate.now(fixed);
        h.execute("insert into stuff(d) values (?)", d);
        assertThat(h.createQuery("select d from stuff").mapTo(LocalDate.class).one()).isEqualTo(d);
    }

    @Test
    public void localDateTime() {
        Handle h = h2Extension.getSharedHandle();

        LocalDateTime d = LocalDateTime.now(fixed);
        h.execute("insert into stuff(ts) values (?)", d);
        assertThat(h.createQuery("select ts from stuff").mapTo(LocalDateTime.class).one()).isEqualTo(d);
    }

    @Test
    public void offsetDateTime() {
        Handle h = h2Extension.getSharedHandle();

        OffsetDateTime dt = OffsetDateTime.now(fixed);
        h.execute("insert into stuff(ts) values (?)", dt);
        assertThat(h.createQuery("select ts from stuff").mapTo(OffsetDateTime.class).one()).isEqualTo(dt);
    }

    @Test
    public void offsetDateTimeLosesOffset() {
        Handle h = h2Extension.getSharedHandle();

        OffsetDateTime dt = OffsetDateTime.now(fixed).withOffsetSameInstant(ZoneOffset.ofHours(-7));
        h.execute("insert into stuff(ts) values (?)", dt);
        assertThat(h.createQuery("select ts from stuff").mapTo(OffsetDateTime.class).one().isEqual(dt)).isTrue();
    }

    @Test
    public void zonedDateTime() {
        Handle h = h2Extension.getSharedHandle();

        ZonedDateTime dt = ZonedDateTime.now(fixed);
        h.execute("insert into stuff(ts) values (?)", dt);
        assertThat(h.createQuery("select ts from stuff").mapTo(ZonedDateTime.class).one()).isEqualTo(dt);
    }

    @Test
    public void zonedDateTimeLosesZone() {
        Handle h = h2Extension.getSharedHandle();

        ZonedDateTime dt = ZonedDateTime.now(fixed).withZoneSameInstant(ZoneId.of("America/Denver"));
        h.execute("insert into stuff(ts) values (?)", dt);
        assertThat(h.createQuery("select ts from stuff").mapTo(ZonedDateTime.class).one().isEqual(dt)).isTrue();
    }

    @Test
    public void localTime() {
        Handle h = h2Extension.getSharedHandle();

        h.execute("create table schedule (start time, end time)");
        LocalTime start = LocalTime.of(8, 30, 0);
        LocalTime end = LocalTime.of(10, 30, 0);
        h.execute("insert into schedule (start, end) values (?,?)", start, end);
        assertThat(h.createQuery("select start from schedule").mapTo(LocalTime.class).one()).isEqualTo(start);
        assertThat(h.createQuery("select end from schedule").mapTo(LocalTime.class).one()).isEqualTo(end);
    }
}

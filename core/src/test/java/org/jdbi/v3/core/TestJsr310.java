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

import static org.assertj.core.api.Assertions.assertThat;


import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.LocalTime;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestJsr310 {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    Handle h;

    @Before
    public void setUp() {
        h = dbRule.getSharedHandle();
        h.execute("create table stuff (ts timestamp, d date)");
    }

    @Test
    public void instant() {
        Instant i = Instant.now();
        h.execute("insert into stuff(ts) values (?)", i);
        assertThat(h.createQuery("select ts from stuff").mapTo(Instant.class).findOnly()).isEqualTo(i);
    }

    @Test
    public void localDate() {
        LocalDate d = LocalDate.now();
        h.execute("insert into stuff(d) values (?)", d);
        assertThat(h.createQuery("select d from stuff").mapTo(LocalDate.class).findOnly()).isEqualTo(d);
    }

    @Test
    public void localDateTime() {
        LocalDateTime d = LocalDateTime.now();
        h.execute("insert into stuff(ts) values (?)", d);
        assertThat(h.createQuery("select ts from stuff").mapTo(LocalDateTime.class).findOnly()).isEqualTo(d);
    }

    @Test
    public void offsetDateTime() {
        OffsetDateTime dt = OffsetDateTime.now();
        h.execute("insert into stuff(ts) values (?)", dt);
        assertThat(h.createQuery("select ts from stuff").mapTo(OffsetDateTime.class).findOnly()).isEqualTo(dt);
    }

    @Test
    public void offsetDateTimeLosesOffset() {
        OffsetDateTime dt = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.ofHours(-7));
        h.execute("insert into stuff(ts) values (?)", dt);
        assertThat(h.createQuery("select ts from stuff").mapTo(OffsetDateTime.class).findOnly().isEqual(dt)).isTrue();
    }

    @Test
    public void zonedDateTime() {
        ZonedDateTime dt = ZonedDateTime.now();
        h.execute("insert into stuff(ts) values (?)", dt);
        assertThat(h.createQuery("select ts from stuff").mapTo(ZonedDateTime.class).findOnly()).isEqualTo(dt);
    }

    @Test
    public void zonedDateTimeLosesZone() {
        ZonedDateTime dt = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("America/Denver"));
        h.execute("insert into stuff(ts) values (?)", dt);
        assertThat(h.createQuery("select ts from stuff").mapTo(ZonedDateTime.class).findOnly().isEqual(dt)).isTrue();
    }

    @Test
    public void localTime(){
        h.execute("create table schedule (start time, end time)");
        LocalTime start = LocalTime.of(8, 30, 0);
        LocalTime end = LocalTime.of(10, 30, 0);
        h.execute("insert into schedule (start, end) values (?,?)", start, end);
        assertThat(h.createQuery("select start from schedule").mapTo(LocalTime.class).findOnly()).isEqualTo(start);
        assertThat(h.createQuery("select end from schedule").mapTo(LocalTime.class).findOnly()).isEqualTo(end);
    }
}

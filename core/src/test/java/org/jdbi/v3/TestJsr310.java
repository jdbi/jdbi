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
package org.jdbi.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.LocalTime;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestJsr310 {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    Handle h;

    @Before
    public void setUp() {
        h = db.getSharedHandle();
        h.execute("create table stuff (ts timestamp, d date)");
    }

    @Test
    public void instant() {
        Instant i = Instant.now();
        h.insert("insert into stuff(ts) values (?)", i);
        assertEquals(i, h.createQuery("select ts from stuff").mapTo(Instant.class).findOnly());
    }

    @Test
    public void localDate() {
        LocalDate d = LocalDate.now();
        h.insert("insert into stuff(d) values (?)", d);
        assertEquals(d, h.createQuery("select d from stuff").mapTo(LocalDate.class).findOnly());
    }

    @Test
    public void localDateTime() {
        LocalDateTime d = LocalDateTime.now();
        h.insert("insert into stuff(ts) values (?)", d);
        assertEquals(d, h.createQuery("select ts from stuff").mapTo(LocalDateTime.class).findOnly());
    }

    @Test
    public void offsetDateTime() {
        OffsetDateTime dt = OffsetDateTime.now();
        h.insert("insert into stuff(ts) values (?)", dt);
        assertEquals(dt, h.createQuery("select ts from stuff").mapTo(OffsetDateTime.class).findOnly());
    }

    @Test
    public void offsetDateTimeLosesOffset() {
        OffsetDateTime dt = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.ofHours(-7));
        h.insert("insert into stuff(ts) values (?)", dt);
        assertTrue(dt.isEqual(h.createQuery("select ts from stuff").mapTo(OffsetDateTime.class).findOnly()));
    }

    @Test
    public void zonedDateTime() {
        ZonedDateTime dt = ZonedDateTime.now();
        h.insert("insert into stuff(ts) values (?)", dt);
        assertEquals(dt, h.createQuery("select ts from stuff").mapTo(ZonedDateTime.class).findOnly());
    }

    @Test
    public void zonedDateTimeLosesZone() {
        ZonedDateTime dt = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("America/Denver"));
        h.insert("insert into stuff(ts) values (?)", dt);
        assertTrue(dt.isEqual(h.createQuery("select ts from stuff").mapTo(ZonedDateTime.class).findOnly()));
    }

    @Test
    public void localTime(){
        h.execute("create table schedule (start time, end time)");
        LocalTime start = LocalTime.of(8, 30, 0);
        LocalTime end = LocalTime.of(10, 30, 0);
        h.insert("insert into schedule (start, end) values (?,?)", start, end);
        assertEquals(start, h.createQuery("select start from schedule").mapTo(LocalTime.class).findOnly());
        assertEquals(end, h.createQuery("select end from schedule").mapTo(LocalTime.class).findOnly());
    }
}

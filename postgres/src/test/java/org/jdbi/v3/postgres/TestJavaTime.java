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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.assertj.core.data.TemporalUnitOffset;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class TestJavaTime {

    @ClassRule
    public static JdbiRule db = PostgresDbRule.rule();

    Handle h;

    @Before
    public void setUp() {
        h = db.getHandle();
        h.useTransaction(th -> {
            th.execute("drop table if exists stuff");
            th.execute("create table stuff (ts timestamp, d date, z text)");
        });
    }

    private TemporalUnitOffset getAllowableOffset() {
        // PostgreSQL seems to not have as much precision on Windows as it does on Linux.
        return within(0, ChronoUnit.MICROS);
    }

    @Test
    public void localDate() {
        LocalDate d = LocalDate.now();
        h.execute("insert into stuff(d) values (?)", d);
        assertThat(h.createQuery("select d from stuff").mapTo(LocalDate.class).one()).isEqualTo(d);
    }

    @Test
    public void localDateTime() {
        LocalDateTime d = LocalDateTime.now();
        h.execute("insert into stuff(ts) values (?)", d);
        assertThat(h.createQuery("select ts from stuff").mapTo(LocalDateTime.class).one()).isCloseTo(d, getAllowableOffset());
    }

    @Test
    public void offsetDateTime() {
        OffsetDateTime dt = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC);
        h.execute("insert into stuff(ts) values (?)", dt);
        assertThat(h.createQuery("select ts from stuff").mapTo(OffsetDateTime.class).one()).isCloseTo(dt, getAllowableOffset());
    }

    @Test
    public void offsetDateTimeLosesOffset() {
        OffsetDateTime dt = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.ofHours(-7));
        h.execute("insert into stuff(ts) values (?)", dt);
        assertThat(h.createQuery("select ts from stuff").mapTo(OffsetDateTime.class).one()).isCloseTo(dt, getAllowableOffset());
    }

    @Test
    public void localTime() {
        h.execute("create table schedule (start time, stop time)");
        LocalTime start = LocalTime.of(8, 30, 0);
        LocalTime stop = LocalTime.of(10, 30, 0);
        h.execute("insert into schedule (start, stop) values (?,?)", start, stop);
        assertThat(h.createQuery("select start from schedule").mapTo(LocalTime.class).one()).isEqualTo(start);
        assertThat(h.createQuery("select stop from schedule").mapTo(LocalTime.class).one()).isEqualTo(stop);
    }

    @Test
    public void instant() {
        final Instant leap = Instant.ofEpochMilli(-14159025000L);
        h.execute("insert into stuff values(?)", leap);
        assertThat(h.createQuery("select ts from stuff").mapTo(Instant.class).one()).isEqualTo(leap);
    }

    @Test
    public void zoneId() {
        final ZoneId zone = ZoneId.systemDefault();
        h.execute("insert into stuff(z) values (?)", zone);
        assertThat(h.createQuery("select z from stuff").mapTo(ZoneId.class).one()).isEqualTo(zone);
    }
}

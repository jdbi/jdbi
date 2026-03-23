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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.AbstractJavaTimeTests;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestJavaTime extends AbstractJavaTimeTests {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg)
        .withPlugin(new PostgresPlugin());

    @BeforeEach
    public void setUp() {
        h = pgExtension.openHandle();
        h.useTransaction(th -> {
            th.execute("drop table if exists stuff");
            th.execute("create table stuff (ts timestamp, d date, t time, z text, tstz timestamptz)");
        });
    }

    @AfterEach
    public void tearDown() {
        h.close();
    }

    /**
     * The resulting OffsetDateTime has the right LocalDateTime (for the default timezone) but the offset is UTC. IAW: It is a mess.
     */
    @Override
    protected void validateOffsetDateTimeLosesOffsetWithTimestamp(OffsetDateTime result, OffsetDateTime expected, ZoneOffset defaultOffset,
        ZoneOffset testOffset) {
        assertThat(result.withOffsetSameLocal(defaultOffset)).isCloseTo(expected, getAllowableOffset());
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    /**
     * postgres always returns UTC (+0) as Offset. See  <a href="https://github.com/pgjdbc/pgjdbc/issues/3943">pgjdbc #3943</a>.
     */
    @Override
    protected void validateOffsetDateTimeTSTZ(OffsetDateTime result, OffsetDateTime expected, ZoneOffset defaultOffset, ZoneOffset testOffset) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());

        // postgres always returns UTC as offset - see https://github.com/pgjdbc/pgjdbc/issues/3943
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
        if (!defaultOffset.equals(ZoneOffset.UTC)) {
            assertThat(result.getOffset()).isNotEqualTo(defaultOffset);
        }
        assertThat(result.getOffset()).isNotEqualTo(testOffset);
    }

    /**
     * The resulting ZonedDateTime has the right LocalDateTime (for the default timezone) but the offset is UTC. IAW: It is a mess.
     */
    @Override
    protected void validateZonedDateTimeLosesZoneWithTimestamp(ZonedDateTime result, ZonedDateTime expected, ZoneId defaultZoneId, ZoneId testZoneId) {
        assertThat(result.withZoneSameLocal(defaultZoneId)).isCloseTo(expected, getAllowableOffset());
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    /**
     * postgres always returns UTC as Zone. See  <a href="https://github.com/pgjdbc/pgjdbc/issues/3943">pgjdbc #3943</a>.
     */
    @Override
    protected void validateZonedDateTimeTSTZ(ZonedDateTime result, ZonedDateTime expected, ZoneId defaultZoneId, ZoneId testZoneId) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());

        assertThat(result.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(result.getZone()).isNotEqualTo(defaultZoneId);
        assertThat(result.getZone()).isNotEqualTo(testZoneId);
    }

    @Test
    public void instantLeap() {
        var type = getTestType(Instant.class);
        var i = Instant.ofEpochMilli(-14159025000L);

        try (Update u = h.createUpdate("insert into stuff(ts) values (?)")) {
            u.bindByType(0, i, type);
            u.execute();
        }

        var result = h.createQuery("select ts from stuff").mapTo(type).one();
        assertThat(result).isCloseTo(i, getAllowableOffset());
    }

    @Test
    public void instantLeapTSTZ() {
        var type = getTestType(Instant.class);
        var i = Instant.ofEpochMilli(-14159025000L);

        try (Update u = h.createUpdate("insert into stuff(tstz) values (?)")) {
            u.bindByType(0, i, type);
            u.execute();
        }

        var result = h.createQuery("select tstz from stuff").mapTo(type).one();
        assertThat(result).isCloseTo(i, getAllowableOffset());
    }
}

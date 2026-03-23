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
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.AbstractJavaTimeTests;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.meta.Legacy;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class TestLegacyJavaTime extends AbstractJavaTimeTests {

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

    @Override
    protected <T> QualifiedType<T> getTestType(Class<T> clazz) {
        return QualifiedType.of(clazz).with(Legacy.class);
    }

    /**
     * Legacy LocalTime mapper only supports second granularity.
     */
    @Override
    protected void validateLocalTime(LocalTime result, LocalTime expected) {
        assertThat(result).isCloseTo(expected, within(0, ChronoUnit.SECONDS));
    }

    /**
     * Legacy OffsetDateTime maps offset to the system default.
     */
    @Override
    protected void validateOffsetDateTimeTSTZ(OffsetDateTime result, OffsetDateTime expected, ZoneOffset defaultOffset, ZoneOffset testOffset) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());

        // legacy mapper maps to the default offset.
        assertThat(result.getOffset()).isEqualTo(defaultOffset);
        assertThat(result.getOffset()).isNotEqualTo(testOffset);
    }

    /**
     * Legacy ZonedDateTime maps to the default zone offset.
     */
    @Override
    protected void validateZonedDateTimeTSTZ(ZonedDateTime result, ZonedDateTime expected, ZoneId defaultZoneId, ZoneId testZoneId) {
        assertThat(result).isCloseTo(expected, getAllowableOffset());

        // multiple zones may match, find all of them and see that the default zone is in there.
        List<String> matchingZoneIds = findZoneIdsFor(result);
        assertThat(matchingZoneIds).doesNotContain(testZoneId.getId());
        assertThat(matchingZoneIds).contains(defaultZoneId.getId());
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

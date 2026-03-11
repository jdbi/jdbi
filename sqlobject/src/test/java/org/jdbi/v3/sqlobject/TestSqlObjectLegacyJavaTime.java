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

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class TestSqlObjectLegacyJavaTime extends AbstractSqlObjectJavaTimeTests {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin());

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
        handle.useTransaction(th -> {
            th.execute("drop table if exists stuff");
            th.execute("create table stuff (ts timestamp, d date, t time(6), z text, tstz timestamp with time zone)");
        });

        dao = handle.attach(LegacyTimeDao.class);
    }

    @AfterEach
    public void tearDown() {
        handle.close();
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
}

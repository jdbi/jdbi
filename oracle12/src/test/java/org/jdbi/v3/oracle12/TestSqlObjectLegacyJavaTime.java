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
package org.jdbi.v3.oracle12;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.jdbi.v3.sqlobject.AbstractSqlObjectJavaTimeTests;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@Testcontainers
public class TestSqlObjectLegacyJavaTime extends AbstractSqlObjectJavaTimeTests {

    static final String CONTAINER_VERSION = "gvenzl/oracle-free:" + System.getProperty("oracle.container.version", "slim-faststart");

    @Container
    static OracleContainer oc = new OracleContainer(CONTAINER_VERSION);

    @RegisterExtension
    JdbiExtension oracleExtension = JdbiTestcontainersExtension.instance(oc)
        .withPlugins(new SqlObjectPlugin(), new OraclePlugin());

    @BeforeEach
    public void setUp() {
        handle = oracleExtension.openHandle();
        handle.useTransaction(th -> {
            th.execute("drop table if exists stuff");
            th.execute("create table stuff (ts timestamp, d date, z varchar2(64), tstz timestamp with time zone)");
        });

        dao = handle.attach(LegacyTimeDao.class);
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    /**
     * Oracle does not support TIME data type.
     */
    @Override
    protected boolean skipLocalTime() {
        return true;
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

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
package org.jdbi.v3.mysql;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.jdbi.v3.core.AbstractJavaTimeTests;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.meta.Legacy;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Tag("slow")
@Testcontainers
public class TestMysqlLegacyJavaTime extends AbstractJavaTimeTests {

    static final String MYSQL_VERSION = System.getProperty("jdbi.test.mysql-version", "mysql");

    @Container
    static JdbcDatabaseContainer<?> dbContainer = new MySQLContainer<>(MYSQL_VERSION);

    @RegisterExtension
    JdbiExtension extension = JdbiTestcontainersExtension.instance(dbContainer)
        .withPlugin(new MysqlPlugin());

    @BeforeEach
    public void setUp() {
        h = extension.openHandle();
        h.useTransaction(th -> {
            th.execute("drop table if exists stuff");
            th.execute("create table stuff (ts timestamp(6), d date, t time(6), z varchar(64))");
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
     * MySQL has no timestamp with time zone
     */
    @Override
    protected boolean skipTSTZ() {
        return true;
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

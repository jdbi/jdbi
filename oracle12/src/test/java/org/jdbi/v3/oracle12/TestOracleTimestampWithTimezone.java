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
import java.time.temporal.ChronoUnit;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.meta.Legacy;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Tag("slow")
@Testcontainers
public class TestOracleTimestampWithTimezone {

    static final String CONTAINER_VERSION = "gvenzl/oracle-free:" + System.getProperty("oracle.container.version", "slim-faststart");

    @Container
    static OracleContainer oc = new OracleContainer(CONTAINER_VERSION);

    @RegisterExtension
    JdbiExtension oracleExtension = JdbiTestcontainersExtension.instance(oc)
        .withPlugin(new SqlObjectPlugin())
        .withInitializer((ds, h) -> {
            h.execute("CREATE TABLE tstz_test (id NUMBER, tstz TIMESTAMP WITH TIME ZONE, ts TIMESTAMP)");
        });

    @Test
    public void offsetDateTimeWithTimestampTz() {
        Handle h = oracleExtension.getSharedHandle();
        OffsetDateTime dt = OffsetDateTime.of(2025, 6, 15, 10, 30, 0, 0, ZoneOffset.ofHours(-7));
        h.execute("INSERT INTO tstz_test (id, tstz) VALUES (1, ?)", dt);
        OffsetDateTime result = h.createQuery("SELECT tstz FROM tstz_test WHERE id = 1").mapTo(OffsetDateTime.class).one();
        assertThat(result.toInstant()).isCloseTo(dt.toInstant(), within(1, ChronoUnit.MICROS));
    }

    @Test
    public void zonedDateTimeWithTimestampTz() {
        Handle h = oracleExtension.getSharedHandle();
        ZonedDateTime dt = ZonedDateTime.of(2025, 6, 15, 10, 30, 0, 0, ZoneId.of("America/Denver"));
        h.execute("INSERT INTO tstz_test (id, tstz) VALUES (1, ?)", dt);
        ZonedDateTime result = h.createQuery("SELECT tstz FROM tstz_test WHERE id = 1").mapTo(ZonedDateTime.class).one();
        assertThat(result.toInstant()).isCloseTo(dt.toInstant(), within(1, ChronoUnit.MICROS));
    }

    @Test
    public void nullOffsetDateTimeWithTimestampTz() {
        Handle h = oracleExtension.getSharedHandle();
        h.execute("INSERT INTO tstz_test (id, tstz) VALUES (1, NULL)");
        OffsetDateTime result = h.createQuery("SELECT tstz FROM tstz_test WHERE id = 1").mapTo(OffsetDateTime.class).one();
        assertThat(result).isNull();
    }

    @Test
    public void offsetDateTimeWithPlainTimestamp() {
        Handle h = oracleExtension.getSharedHandle();
        OffsetDateTime dt = OffsetDateTime.now(ZoneId.systemDefault()).withOffsetSameInstant(ZoneOffset.ofHours(-10));

        var type = QualifiedType.of(OffsetDateTime.class).with(Legacy.class);

        try (Update stmt = h.createUpdate("INSERT INTO tstz_test (id, ts) VALUES (1, ?)")) {
            stmt.bindByType(0, dt, type);
            stmt.execute();
        }

        OffsetDateTime result = h.createQuery("SELECT ts FROM tstz_test WHERE id = 1").mapTo(type).one();
        assertThat(result.toInstant()).isCloseTo(dt.toInstant(), within(1, ChronoUnit.SECONDS));
    }

    interface TstzDao {
        @SqlUpdate("INSERT INTO tstz_test (id, ts) VALUES (:id, :ts)")
        void insert(@Bind("id") int id, @Legacy @Bind("ts") OffsetDateTime ts);

        @SqlQuery("SELECT ts FROM tstz_test WHERE id = :id")
        @Legacy
        OffsetDateTime findTs(@Bind("id") int id);
    }

    @Test
    public void offsetDateTimeWithPlainTimestampSqlObject() {
        OffsetDateTime dt = OffsetDateTime.now(ZoneId.systemDefault()).withOffsetSameInstant(ZoneOffset.ofHours(-10));
        TstzDao dao = oracleExtension.getSharedHandle().attach(TstzDao.class);
        dao.insert(2, dt);
        OffsetDateTime result = dao.findTs(2);
        assertThat(result.toInstant()).isCloseTo(dt.toInstant(), within(1, ChronoUnit.SECONDS));
    }
}

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
import java.time.temporal.ChronoUnit;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.assertj.core.data.TemporalUnitOffset;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Update;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TestJavaTime {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg)
        .withPlugin(new PostgresPlugin());

    private Handle h;

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

    TemporalUnitOffset getAllowableOffset() {
        return within(0, ChronoUnit.MICROS);
    }

    @Test
    void instantLeap() {
        var i = Instant.ofEpochMilli(-14159025000L);

        try (Update u = h.createUpdate("insert into stuff(ts) values (?)")) {
            u.bindByType(0, i, Instant.class);
            u.execute();
        }

        var result = h.createQuery("select ts from stuff").mapTo(Instant.class).one();
        assertThat(result).isCloseTo(i, getAllowableOffset());
    }

    @Test
    void instantLeapTSTZ() {
        var i = Instant.ofEpochMilli(-14159025000L);

        try (Update u = h.createUpdate("insert into stuff(tstz) values (?)")) {
            u.bindByType(0, i, Instant.class);
            u.execute();
        }

        var result = h.createQuery("select tstz from stuff").mapTo(Instant.class).one();
        assertThat(result).isCloseTo(i, getAllowableOffset());
    }
}

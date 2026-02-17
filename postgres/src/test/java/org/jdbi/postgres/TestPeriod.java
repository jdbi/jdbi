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
package org.jdbi.postgres;

import java.time.Period;
import java.util.List;

import com.google.common.collect.ImmutableList;
import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.core.Handle;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestPeriod {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg).withPlugins(new SqlObjectPlugin(), new PostgresPlugin())
        .withInitializer((ds, h) -> h.useTransaction(th -> {
            th.execute("drop table if exists intervals");
            th.execute("create table intervals(id int not null, foo interval)");

            // Can be periods.
            th.execute("insert into intervals(id, foo) values(1, interval '2 years -3 months 40 days')");
            th.execute("insert into intervals(id, foo) values(2, interval '7 days')");

            // Can't be.
            th.execute("insert into intervals(id, foo) values(3, interval '10 years -3 months 100 seconds')");
        }));

    private final Period testPeriod = Period.of(1776, 7, 4);

    private Handle handle;

    @BeforeEach
    public void setUp() {
        this.handle = pgExtension.getSharedHandle();
    }

    @Test
    public void testReadsViaFluentAPI() {
        List<Period> periods = handle.createQuery("select foo from intervals where id = 1 or id = 2 order by id")
            .mapTo(Period.class)
            .list();

        assertThat(periods).isEqualTo(ImmutableList.of(
            Period.of(1, 9, 40),
            Period.of(0, 0, 7)
        ));
    }

    @Test
    public void testTrivialPeriod() {
        handle.execute("insert into intervals(id, foo) values(?, ?)", 4, Period.of(0, 0, 0));
        Period p = handle.createQuery("select foo from intervals where id=?")
                .bind(0, 4)
                .mapTo(Period.class)
                .one();
        assertThat(p.isZero()).isTrue();
    }

    @Test
    public void testHandlesNulls() {
        handle.execute("insert into intervals(id, foo) values(?, ?)", 5, null);
        final Period p = handle.createQuery("select foo from intervals where id=?")
                .bind(0, 5)
                .mapTo(Period.class)
                .one();
        assertThat(p).isNull();
    }

    @Test
    public void testWritesViaFluentApi() {
        handle.execute("insert into intervals(id, foo) values(?, ?)", 6, testPeriod);
        final Period p = handle.createQuery("select foo from intervals where id=?")
                .bind(0, 6)
                .mapTo(Period.class)
                .one();
        assertThat(p).isEqualTo(testPeriod);
    }

    @Test
    public void testInvalidPeriod() {
        assertThatThrownBy(() -> handle.createQuery("select foo from intervals where id=?")
            .bind(0, 3) // The bad one.
            .mapTo(Period.class)
            .one()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testNegativePeriod() {
        handle.execute("insert into intervals(id, foo) values(?, interval '-3 years -1 month 2 days')", 7);
        final Period p = handle.createQuery("select foo from intervals where id=?")
            .bind(0, 7)
                .mapTo(Period.class)
                .one();
        assertThat(p).isEqualTo(Period.of(-3, -1, 2));
    }
}

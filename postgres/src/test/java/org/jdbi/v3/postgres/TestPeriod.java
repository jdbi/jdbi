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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Period;
import java.util.List;

import com.google.common.collect.ImmutableList;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class TestPeriod {
    @ClassRule
    public static JdbiRule postgresDbRule = PostgresDbRule.rule();

    private Handle handle;

    private final Period testPeriod = Period.of(1776, 7, 4);

    @Before
    public void setUp() throws Exception {
        handle = postgresDbRule.getHandle();
        handle.useTransaction(h -> {
            h.execute("drop table if exists intervals");
            h.execute("create table intervals(id int not null, foo interval)");

            // Can be periods.
            h.execute("insert into intervals(id, foo) values(1, interval '2 years -3 months 40 days')");
            h.execute("insert into intervals(id, foo) values(2, interval '7 days')");

            // Can't be.
            h.execute("insert into intervals(id, foo) values(3, interval '10 years -3 months 100 seconds')");
        });
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
                .findOnly();
        assertThat(p.isZero());
    }

    @Test
    public void testHandlesNulls() {
        handle.execute("insert into intervals(id, foo) values(?, ?)", 5, null);
        final Period p = handle.createQuery("select foo from intervals where id=?")
                .bind(0, 5)
                .mapTo(Period.class)
                .findOnly();
        assertThat(p).isNull();
    }

    @Test
    public void testWritesViaFluentApi() {
        handle.execute("insert into intervals(id, foo) values(?, ?)", 6, testPeriod);
        final Period p = handle.createQuery("select foo from intervals where id=?")
                .bind(0, 6)
                .mapTo(Period.class)
                .findOnly();
        assertThat(p).isEqualTo(testPeriod);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPeriod() {
        handle.createQuery("select foo from intervals where id=?")
                .bind(0, 3) // The bad one.
                .mapTo(Period.class)
                .findOnly();
    }

    @Test
    public void testNegativePeriod() {
        handle.execute("insert into intervals(id, foo) values(?, interval '-3 years -1 month 2 days')", 7);
        final Period p = handle.createQuery("select foo from intervals where id=?")
            .bind(0, 7)
                .mapTo(Period.class)
                .findOnly();
        assertThat(p).isEqualTo(Period.of(-3, -1, 2));
    }
}

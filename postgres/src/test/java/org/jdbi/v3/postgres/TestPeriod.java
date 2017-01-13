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

import com.google.common.collect.ImmutableList;
import org.jdbi.v3.core.Handle;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.time.Period;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPeriod {
    @ClassRule
    public static PostgresDbRule postgresDbRule = new PostgresDbRule();

    private Handle handle;

    private final Period testPeriod = Period.of(1776, 7, 4);

    @Before
    public void setUp() throws Exception {
        handle = IntervalTestCommon.setUp(postgresDbRule);
    }

    @Test
    public void testReadsViaFluentAPI() {
        List<Period> periods = handle.createQuery("select foo from intervals where id = 3 or id = 4 order by id")
                .mapTo(Period.class)
                .list();

        assertThat(periods).isEqualTo(ImmutableList.of(
                Period.of(1, 9, 40),
                Period.of(0, 0, 7)
        ));
    }

    @Test
    public void testTrivialPeriod() {
        handle.insert("insert into intervals(id, foo) values(?, ?)", 6, Period.of(0, 0, 0));
        Period p = handle.createQuery("select foo from intervals where id=?")
                .bind(0, 6)
                .mapTo(Period.class)
                .findOnly();
        assertThat(p.isZero());
    }

    @Test
    public void testHandlesNulls() {
        handle.insert("insert into intervals(id, foo) values(?, ?)", 7, null);
        final Period p = handle.createQuery("select foo from intervals where id=?")
                .bind(0, 7)
                .mapTo(Period.class)
                .findOnly();
        assertThat(p).isNull();
    }

    @Test
    public void testWritesViaFluentApi() {
        handle.insert("insert into intervals(id, foo) values(?, ?)", 8, testPeriod);
        final Period p = handle.createQuery("select foo from intervals where id=?")
                .bind(0, 8)
                .mapTo(Period.class)
                .findOnly();
        assertThat(p).isEqualTo(testPeriod);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPeriod() {
        handle.createQuery("select foo from intervals where id=?")
                .bind(0, 5)
                .mapTo(Period.class)
                .findOnly();
    }

    @Test
    public void testNegativePeriod() {
        handle.insert("insert into intervals(id, foo) values(?, interval '-3 years -1 month 2 days')", 10);
        final Period p = handle.createQuery("select foo from intervals where id=?")
            .bind(0, 10)
                .mapTo(Period.class)
                .findOnly();
        assertThat(p).isEqualTo(Period.of(-3, -1, 2));
    }
}

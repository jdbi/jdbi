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

import org.jdbi.v3.core.Handle;

/**
 * Common test setup for Period and Duration tests.
 */
public class IntervalTestCommon {
    public static Handle setUp(final PostgresDbRule postgresDbRule) throws Exception {
        final Handle handle = postgresDbRule.getSharedHandle();
        handle.useTransaction(h -> {
            h.execute("drop table if exists intervals");
            h.execute("create table intervals(id int not null, foo interval)");

            // Can be durations.
            h.execute("insert into intervals(id, foo) values(1, interval '1 day 15:00:00')");
            h.execute("insert into intervals(id, foo) values(2, interval '40 days 22 minutes')");

            // Can be intervals.
            h.execute("insert into intervals(id, foo) values(3, interval '2 years -3 months 40 days')");
            h.execute("insert into intervals(id, foo) values(4, interval '7 days')");

            // Can be neither.
            h.execute("insert into intervals(id, foo) values(5, interval '10 years -3 months 100 seconds')");
        });
        return handle;
    }
}

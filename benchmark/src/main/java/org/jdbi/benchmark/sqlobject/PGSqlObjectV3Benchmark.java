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
package org.jdbi.benchmark.sqlobject;

import org.jdbi.core.Jdbi;
import org.jdbi.core.internal.exceptions.Unchecked;
import org.jdbi.postgres.PostgresPlugin;

public class PGSqlObjectV3Benchmark extends BaseSqlObjectV3Benchmark {
    @Override
    protected Jdbi createJdbi() {
        return Unchecked.supplier(() -> Jdbi.create(PGSqlObjectV2Benchmark.PROVIDER.createDefaultDataSource())
            .installPlugin(new PostgresPlugin())).get();
    }

    @Override
    protected void createTable() {
        handle.execute("drop table if exists tbl");
        handle.execute("create table tbl (id serial primary key, name varchar, description varchar)");
    }
}

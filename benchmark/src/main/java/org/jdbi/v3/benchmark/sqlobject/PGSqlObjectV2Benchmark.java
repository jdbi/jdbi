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
package org.jdbi.v3.benchmark.sqlobject;

import com.opentable.db.postgres.embedded.PreparedDbProvider;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.skife.jdbi.v2.DBI;

public class PGSqlObjectV2Benchmark extends BaseSqlObjectV2Benchmark {
    static final PreparedDbProvider PROVIDER = PreparedDbProvider.forPreparer(p -> {});

    @Override
    protected DBI createJdbi() {
        return Unchecked.supplier(() -> new DBI(PROVIDER.createDataSource())).get();
    }

    @Override
    protected void createTable() {
        handle.execute("drop table if exists tbl");
        handle.execute("create table tbl (id serial primary key, name varchar, description varchar)");
    }
}

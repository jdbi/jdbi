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

import java.util.UUID;

import org.h2.Driver;
import org.jdbi.v3.core.Jdbi;

public class H2SqlObjectV3Benchmark extends BaseSqlObjectV3Benchmark {
    static {
        Driver.load();
    }

    @Override
    protected Jdbi createJdbi() {
        return Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=10");
    }

    @Override
    protected void createTable() {
        handle.execute("drop table if exists tbl");
        handle.execute("create table tbl (id identity, name varchar, description varchar)");
    }
}

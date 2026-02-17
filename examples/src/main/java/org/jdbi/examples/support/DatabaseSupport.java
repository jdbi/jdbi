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
package org.jdbi.examples.support;

import java.util.function.Consumer;

import de.softwareforge.testing.postgres.embedded.DatabaseInfo;
import de.softwareforge.testing.postgres.embedded.DatabaseManager;
import de.softwareforge.testing.postgres.embedded.EmbeddedPostgres;
import org.jdbi.core.Jdbi;
import org.jdbi.postgres.PostgresPlugin;
import org.jdbi.sqlobject.SqlObjectPlugin;

public final class DatabaseSupport {

    private DatabaseSupport() {
        throw new AssertionError("DatabaseSupport can not be instantiated");
    }

    public static void withDatabase(Consumer<Jdbi> jdbiConsumer) throws Exception {
        try (DatabaseManager manager = DatabaseManager.singleDatabase()
            // same as EmbeddedPostgres.defaultInstance()
            .withInstancePreparer(EmbeddedPostgres.Builder::withDefaults)
            .build()
            .start()) {
            DatabaseInfo databaseInfo = manager.getDatabaseInfo();
            Jdbi jdbi = Jdbi.create(databaseInfo.asDataSource());
            jdbi.installPlugin(new SqlObjectPlugin())
                .installPlugin(new PostgresPlugin());

            jdbiConsumer.accept(jdbi);
        }
    }
}

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
package org.jdbi.v3.examples.order;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.function.Consumer;

import de.softwareforge.testing.postgres.embedded.DatabaseInfo;
import de.softwareforge.testing.postgres.embedded.DatabaseManager;
import de.softwareforge.testing.postgres.embedded.EmbeddedPostgres;
import org.jdbi.v3.core.Jdbi;

public final class OrderSupport {
    private static final SecureRandom RANDOM = new SecureRandom();

    private OrderSupport() {
        throw new AssertionError("OrderSupport can not be instantiated");
    }

    public static void withOrders(Consumer<Jdbi> jdbiConsumer) throws Exception {
        try (DatabaseManager manager = DatabaseManager.singleDatabase()
            // same as EmbeddedPostgres.defaultInstance()
            .withInstancePreparer(EmbeddedPostgres.Builder::withDefaults)
            .build()
            .start()) {
            DatabaseInfo databaseInfo = manager.getDatabaseInfo();
            Jdbi jdbi = Jdbi.create(databaseInfo.asDataSource());

            createTables(jdbi);
            populateOrders(jdbi, 3, 20);

            jdbi.registerRowMapper(new OrderMapper());

            jdbiConsumer.accept(jdbi);
        }
    }

    public static void createTables(Jdbi jdbi) {
        jdbi.withHandle(
            handle -> handle.execute("CREATE TABLE orders (id INT, user_id INT, comment VARCHAR, address VARCHAR)"));
    }

    public static void populateOrders(Jdbi jdbi, int orderCount, int userIdCount) {

        jdbi.withHandle(
            handle -> {
                for (int j = 0; j < userIdCount; j++) {
                    int userId = RANDOM.nextInt(10_000);
                    for (int i = 0; i < orderCount; i++) {
                        handle.createUpdate("INSERT INTO orders (id, user_id, comment, address) VALUES (:id, :user_id, :comment, :address)")
                            .bind("id", RANDOM.nextInt(1_000_000))
                            .bind("user_id", userId)
                            .bind("comment", UUID.randomUUID().toString().substring(0, 5))
                            .bind("address", UUID.randomUUID().toString().substring(0, 5))
                            .execute();
                    }
                }
                return null;
            });
    }
}

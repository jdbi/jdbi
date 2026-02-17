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
package org.jdbi.examples;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.jdbi.sqlobject.SingleValue;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;

import static org.jdbi.examples.support.DatabaseSupport.withDatabase;

/**
 * Defines a custom SQL array type and maps it onto a database array column.
 */
@SuppressWarnings({"PMD.SystemPrintln"})
public final class CustomSqlArrayType {

    // run in UTC timezone to get same result back.
    static {
        System.setProperty("user.timezone", "UTC");
    }

    private CustomSqlArrayType() {
        throw new AssertionError("CustomSqlArrayType can not be instantiated");
    }

    public static void main(String... args) throws Exception {

        // test data
        final Instant[] testInstants = {
            Instant.EPOCH,
            Instant.now().truncatedTo(ChronoUnit.MILLIS),
            Instant.ofEpochSecond(Integer.MIN_VALUE),
            Instant.ofEpochSecond(Integer.MAX_VALUE),
            // amazingly, according to ISO 8601, everything before 1583 is a mess...
            Instant.parse("1583-01-01T00:00:00Z"),
            // and anything after 9999 is as well...
            Instant.parse("9999-12-31T23:59:59Z"),
        };

        withDatabase(jdbi -> {
            // create database table
            jdbi.inTransaction(th -> {
                th.execute("DROP TABLE IF EXISTS custom_sql");
                th.execute("CREATE TABLE custom_sql (t TIMESTAMP[])");
                return null;
            });

            // register the custom array type to map instant to timestamp
            jdbi.registerArrayType(Instant.class, "timestamp");

            CustomSqlObject dao = jdbi.onDemand(CustomSqlObject.class);

            // write data (with a sql object)
            dao.insertInstantArray(testInstants);

            // fetch results back
            List<Instant> result = dao.fetchInstantList();

            // display results
            result.forEach(r -> System.out.printf("%s%n", r));
        });
    }

    public interface CustomSqlObject {

        @SqlUpdate("INSERT INTO custom_sql (t) VALUES (:instants)")
        void insertInstantArray(Instant[] instants);

        @SqlQuery("SELECT t FROM custom_sql")
        @SingleValue
        List<Instant> fetchInstantList();
    }

}

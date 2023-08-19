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
module org.jdbi.v3.testing {
    requires java.logging;

    requires static com.h2database;
    requires static jakarta.annotation;
    requires static org.flywaydb.core;
    requires static org.postgresql.jdbc;
    requires static org.xerial.sqlitejdbc;

    requires transitive java.sql;

    requires transitive org.jdbi.v3.core;
    requires transitive static de.softwareforge.testing.postgres;
    requires transitive static junit;
    requires transitive static org.junit.jupiter.api;
    requires transitive static otj.pg.embedded;

    exports org.jdbi.v3.testing;
    exports org.jdbi.v3.testing.junit5;
}

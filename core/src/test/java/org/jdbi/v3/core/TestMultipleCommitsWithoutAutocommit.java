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
package org.jdbi.v3.core;

import org.assertj.core.api.Assertions;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMultipleCommitsWithoutAutocommit {
    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();
    @BeforeEach
    public void startUp() {
        Assertions.setMaxStackTraceElementsDisplayed(100);
    }

    @Test
    public void testMultipleCommitsWithoutAutocommit() throws Exception {
        Jdbi jdbi = Jdbi.create(() -> {
            // create connection with auto-commit == false
            Connection connection = DriverManager.getConnection(h2Extension.getUri());
            connection.setAutoCommit(false);
            return connection;
        });
        try (Handle handle = jdbi.open()) {
            handle.execute("create table names(name varchar)");
            handle.commit();
            handle.execute("insert into names (name) values ('Kafka')");
            handle.commit();
        }
        try (Handle handle = jdbi.open()) {
            assertThat(handle.createQuery("select count(1) from names").mapTo(Integer.class).one())
                .isEqualTo(1);
        }
    }
}

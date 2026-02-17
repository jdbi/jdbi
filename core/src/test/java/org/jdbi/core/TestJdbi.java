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
package org.jdbi.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.jdbi.core.internal.testing.H2DatabaseExtension;
import org.jdbi.core.statement.Query;
import org.jdbi.core.statement.StatementCustomizers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestJdbi {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER);

    @Test
    public void testDataSourceConstructor() {
        Jdbi db = Jdbi.create(h2Extension.getUri());
        try (Handle h = db.open()) {
            assertThat(h).isNotNull();
        }
    }

    @Test
    public void testConnectionConstructor() throws SQLException {
        ConnectionFactory cf = () -> DriverManager.getConnection(h2Extension.getUri());
        Connection connection = cf.openConnection();

        Jdbi db = Jdbi.create(connection);

        try (Handle h = db.open()) {
            assertThat(h).isNotNull();
        }

        assertThat(connection.isClosed()).isFalse();
    }

    @Test
    public void testConnectionFactoryCtor() {
        Jdbi db = Jdbi.create(() -> {
            try {
                return DriverManager.getConnection(h2Extension.getUri());
            } catch (SQLException e) {
                throw new ConnectionException(e);
            }
        });
        try (Handle h = db.open()) {
            assertThat(h).isNotNull();
        }
    }

    @Test
    public void testCorrectExceptionOnSQLException() {
        Jdbi db = Jdbi.create(() -> {
            throw new SQLException();
        });

        assertThatThrownBy(db::open).isInstanceOf(ConnectionException.class);
    }

    @Test
    public void testWithHandle() {
        Jdbi db = h2Extension.getJdbi();
        String value = db.withHandle(handle -> {
            handle.execute("insert into something (id, name) values (1, 'Brian')");
            return handle.createQuery("select name from something where id = 1").mapToBean(Something.class).one().getName();
        });
        assertThat(value).isEqualTo("Brian");
    }

    @Test
    public void testUseHandle() {
        Jdbi db = h2Extension.getJdbi();
        db.useHandle(handle -> {
            handle.execute("insert into something (id, name) values (1, 'Brian')");
            String value = handle.createQuery("select name from something where id = 1").mapToBean(Something.class).one().getName();
            assertThat(value).isEqualTo("Brian");
        });
    }

    @Test
    public void test10StatementsUseHandle() {
        Jdbi db = h2Extension.getJdbi();
        h2Extension.getSharedHandle().execute("insert into something (id, name) values (1, 'Brian')");
        h2Extension.getSharedHandle().execute("insert into something (id, name) values (2, 'Steven')");

        db.useHandle(handle -> {
            // run a few of statements
            for (int i = 0; i < 10; i++) {
                // auto resource management is ok
                Query q = handle.createQuery("select * from something order by id asc");
                Optional<String> value = q.mapToBean(Something.class)
                        .stream().findFirst()
                        .map(Something::getName);

                assertThat(value)
                        .isPresent()
                        .contains("Brian");
            }
        });
    }

    @Test
    public void testGlobalStatementCustomizers() {
        h2Extension.getJdbi().addCustomizer(StatementCustomizers.maxRows(1))
            .useHandle(handle -> {
                handle.execute("insert into something (id, name) values (?, ?)", 1, "hello");
                handle.execute("insert into something (id, name) values (?, ?)", 2, "world");

                List<Something> rs = handle.createQuery("select id, name from something")
                    .mapToBean(Something.class)
                    .list();

                assertThat(rs).hasSize(1);
            });
    }
}


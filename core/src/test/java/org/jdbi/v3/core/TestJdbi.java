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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.StatementCustomizers;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestJdbi {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    private Handle handle;

    @After
    public void doTearDown() throws Exception {
        if (handle != null) {
            handle.close();
        }
    }

    @Test
    public void testDataSourceConstructor() {
        Jdbi db = Jdbi.create(this.dbRule.getConnectionString());
        try (Handle h = db.open()) {
            assertThat(h).isNotNull();
        }
    }

    @Test
    public void testConnectionConstructor() throws SQLException {
        Connection connection = this.dbRule.getConnectionFactory().openConnection();

        Jdbi db = Jdbi.create(connection);

        try (Handle h = db.open()) {
            assertThat(h).isNotNull();
        }

        assertThat(connection.isClosed()).isFalse();
    }

    public void testConnectionFactoryCtor() {
        Jdbi db = Jdbi.create(() -> {
            try {
                return DriverManager.getConnection(this.dbRule.getConnectionString());
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
        Jdbi db = Jdbi.create(this.dbRule.getConnectionString());
        String value = db.withHandle(handle -> {
            handle.execute("insert into something (id, name) values (1, 'Brian')");
            return handle.createQuery("select name from something where id = 1").mapToBean(Something.class).findOnly().getName();
        });
        assertThat(value).isEqualTo("Brian");
    }

    @Test
    public void testUseHandle() {
        Jdbi db = Jdbi.create(this.dbRule.getConnectionString());
        db.useHandle(handle -> {
            handle.execute("insert into something (id, name) values (1, 'Brian')");
            String value = handle.createQuery("select name from something where id = 1").mapToBean(Something.class).findOnly().getName();
            assertThat(value).isEqualTo("Brian");
        });
    }

    @Test
    public void testGlobalStatementCustomizers() throws Exception {
        dbRule.getJdbi().addCustomizer(StatementCustomizers.maxRows(1));

        handle = dbRule.openHandle();

        handle.execute("insert into something (id, name) values (?, ?)", 1, "hello");
        handle.execute("insert into something (id, name) values (?, ?)", 2, "world");

        List<Something> rs = handle.createQuery("select id, name from something")
                .mapToBean(Something.class)
                .list();

        assertThat(rs).hasSize(1);
    }

}


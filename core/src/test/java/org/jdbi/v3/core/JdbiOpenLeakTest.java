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
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.core.transaction.LocalTransactionHandler;
import org.jdbi.v3.core.transaction.TransactionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JdbiOpenLeakTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER);

    @Test
    void cleanupCustomizeThrows() throws Exception {

        var jdbiPlugin = new JdbiPlugin() {
            @Override
            public Connection customizeConnection(final Connection conn) throws SQLException {
                throw new CosmicRayException();
            }
        };

        h2Extension.getJdbi().installPlugin(jdbiPlugin);

        h2Extension.clearLastConnection();
        assertThatThrownBy(() -> h2Extension.getJdbi().open())
                .isInstanceOf(CosmicRayException.class);

        assertThat(h2Extension.getLastConnection()).isNotNull(); // has been created
        assertThat(h2Extension.getLastConnection().isClosed()).isTrue(); // has been closed
    }

    @Test
    void cleanupIsClosedThrows() throws Exception {
        Connection conn = mock(Connection.class);
        AtomicBoolean leased = new AtomicBoolean(false);
        AtomicBoolean handleCleaned = new AtomicBoolean(false);

        Jdbi jdbi = Jdbi.create(() -> {
            leased.set(true);
            return conn;
        });

        when(conn.isClosed()).thenThrow(CosmicRayException.class);
        assertThat(leased.get()).isFalse();
        assertThat(handleCleaned.get()).isFalse();

        try (Handle h = jdbi.open()) {
            h.addCleanable(() -> handleCleaned.set(true));
        }

        assertThat(handleCleaned.get()).isTrue();
        assertThat(leased.get()).isTrue();
        verify(conn).close();
    }

    @Test
    public void testIssue2446() throws Exception {

        h2Extension.getSharedHandle().execute("insert into something (id, name) values (1, 'Brian')");

        Jdbi jdbi = h2Extension.getJdbi();
        final ExplodeInSpecializeTransactionHandler handler = new ExplodeInSpecializeTransactionHandler();
        h2Extension.getJdbi().setTransactionHandler(handler);

        assertThatThrownBy(() -> {
            String value;
            h2Extension.clearLastConnection(); // reset connection

            try (Handle handle = jdbi.open()) {
                value = handle.createQuery("select name from something where id = 1").mapToBean(Something.class).one().getName();
            }
            assertThat(value).isEqualTo("Brian");
        }).isInstanceOf(ConnectionException.class)
                .hasMessageContaining("transaction specialization failure!")
                .hasCauseInstanceOf(SQLException.class);

        // see if the c'tor leaked a connection
        assertThat(h2Extension.getLastConnection()).isNotNull(); // connection has been checked out
        assertThat(h2Extension.getLastConnection().isClosed()).isTrue(); // connection has been closed
    }

    static class CosmicRayException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    static class ExplodeInSpecializeTransactionHandler extends LocalTransactionHandler {
        @Override
        public TransactionHandler specialize(Handle handle) throws SQLException {
            throw new SQLException("transaction specialization failure!");
        }
    }
}

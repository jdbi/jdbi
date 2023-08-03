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

import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.core.statement.Cleanable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JdbiOpenLeakTest {

    Connection conn = mock(Connection.class);
    boolean leased = false;
    boolean handleCleaned = false;
    Jdbi jdbi = Jdbi.create(() -> {
        leased = true;
        return conn;
    });

    @Test
    void cleanupCustomizeThrows() throws Exception {
        assertThat(leased).isFalse();

        jdbi.installPlugin(new JdbiPlugin() {
            @Override
            public Connection customizeConnection(final Connection conn) throws SQLException {
                throw new CosmicRayException();
            }
        });

        assertThatThrownBy(jdbi::open)
                .isInstanceOf(CosmicRayException.class);
        assertThat(leased).isTrue();
        verify(conn).close();
    }

    @Test
    void cleanupIsClosedThrows() throws Exception {
        when(conn.isClosed()).thenThrow(CosmicRayException.class);
        assertThat(leased).isFalse();
        assertThat(handleCleaned).isFalse();

        try (Handle h = jdbi.open()) {
            h.addCleanable(new Cleanable() {
                @Override
                public void close() throws SQLException {
                    handleCleaned = true;
                }
            });
        }

        assertThat(handleCleaned).isTrue();
        assertThat(leased).isTrue();
        verify(conn).close();
    }

    class CosmicRayException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}

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
import java.sql.SQLException;

import org.jdbi.core.spi.JdbiPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TestCloseConnection {
    @Mock Connection outerCxn;
    @Mock Connection innerCxn;

    @Test
    void closeCustomizedConnection() throws Exception {
        final Jdbi jdbi = Jdbi.create(() -> outerCxn);
        jdbi.installPlugin(new JdbiPlugin() {
            @Override
            public Connection customizeConnection(final Connection conn) throws SQLException {
                assertThat(conn).isSameAs(outerCxn);
                return innerCxn;
            }
        });
        jdbi.useHandle(h -> {});
        verify(outerCxn, never()).close();
        verify(innerCxn).close();
    }

    @Test
    void customizeConnectionThrows() throws Exception {
        final Jdbi jdbi = Jdbi.create(() -> outerCxn);
        final var t = new IllegalArgumentException();
        jdbi.installPlugin(new JdbiPlugin() {
            @Override
            public Connection customizeConnection(final Connection conn) throws SQLException {
                throw t;
            }
        });
        assertThatThrownBy(() -> jdbi.useHandle(h -> {}))
                .isSameAs(t);
        verify(outerCxn).close();
        verify(innerCxn, never()).close();
    }
}

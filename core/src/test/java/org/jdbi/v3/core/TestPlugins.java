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

import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class TestPlugins {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @Test
    public void testCustomizeHandle() {
        Handle h = mock(Handle.class);

        h2Extension.getJdbi().installPlugin(new JdbiPlugin() {
            @Override
            public Handle customizeHandle(Handle handle) {
                handle.close(); // otherwise the handle leaks out
                return h;
            }
        });

        try (Handle testHandle = h2Extension.openHandle()) {
            assertThat(h).isSameAs(testHandle);
        }
    }

    @Test
    public void testCustomizeConnection() {
        Connection c = mock(Connection.class);

        h2Extension.getJdbi().installPlugin(new JdbiPlugin() {
            @Override
            public Connection customizeConnection(Connection conn) throws SQLException {
                conn.close(); // otherwise the connection leaks out
                return c;
            }
        });

        try (Handle testHandle = h2Extension.openHandle()) {
            assertThat(c).isSameAs(testHandle.getConnection());
        }
    }
}

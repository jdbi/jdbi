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

import org.jdbi.core.internal.testing.H2DatabaseExtension;
import org.jdbi.core.spi.JdbiPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPlugins {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @Test
    public void testCustomizeHandle() throws SQLException {
        ConnectionFactory cf = () -> DriverManager.getConnection(h2Extension.getUri());
        Connection connection = cf.openConnection();
        Jdbi db = Jdbi.create(connection);
        try (Handle h = db.open()) {
            h2Extension.getJdbi().installPlugin(new JdbiPlugin() {
                @Override
                public Handle customizeHandle(Handle handle) {
                    handle.close(); // otherwise the handle leaks out
                    return h;
                }
            });
            Handle testHandle = h2Extension.openHandle();
            assertThat(h).isSameAs(testHandle);
        }
    }

    @Test
    public void testCustomizeConnection() throws SQLException {
        ConnectionFactory cf = () -> DriverManager.getConnection(h2Extension.getUri());
        Connection connection = cf.openConnection();
        Jdbi db = Jdbi.create(connection);
        try (Handle h = db.open()) {
            h2Extension.getJdbi().installPlugin(new JdbiPlugin() {
                @Override
                public Connection customizeConnection(Connection conn) throws SQLException {
                    conn.close(); // otherwise the connection leaks out
                    return connection;
                }
            });
            try (Handle testHandle = h2Extension.openHandle()) {
                assertThat(connection).isSameAs(testHandle.getConnection());
            }
        }
    }
}

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.sql.Connection;

import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.Rule;
import org.junit.Test;

public class TestPlugins {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testCustomizeHandle() throws Exception {
        Handle h = mock(Handle.class);

        db.getJdbi().installPlugin(new JdbiPlugin() {
            @Override
            public Handle customizeHandle(Handle handle) {
                return h;
            }
        });

        assertThat(h).isSameAs(db.getJdbi().open());
    }

    @Test
    public void testCustomizeConnection() throws Exception {
        Connection c = mock(Connection.class);

        db.getJdbi().installPlugin(new JdbiPlugin() {
            @Override
            public Connection customizeConnection(Connection conn) {
                return c;
            }
        });

        assertThat(c).isSameAs(db.getJdbi().open().getConnection());
    }
}

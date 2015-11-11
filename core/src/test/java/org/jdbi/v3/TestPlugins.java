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
package org.jdbi.v3;

import static org.junit.Assert.assertSame;

import java.sql.Connection;

import org.easymock.EasyMock;
import org.jdbi.v3.spi.JdbiPlugin;
import org.junit.Rule;
import org.junit.Test;

public class TestPlugins {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testCustomizeHandle() throws Exception {
        Handle h = EasyMock.createNiceMock(Handle.class);
        EasyMock.replay(h);

        db.getDbi().installPlugin(new JdbiPlugin() {
            @Override
            public Handle customizeHandle(Handle handle) {
                return h;
            }
        });

        assertSame(h, db.getDbi().open());
    }

    @Test
    public void testCustomizeConnection() throws Exception {
        Connection c = EasyMock.createNiceMock(Connection.class);
        EasyMock.replay(c);

        db.getDbi().installPlugin(new JdbiPlugin() {
            @Override
            public Connection customizeConnection(Connection conn) {
                return c;
            }
        });

        assertSame(c, db.getDbi().open().getConnection());
    }
}

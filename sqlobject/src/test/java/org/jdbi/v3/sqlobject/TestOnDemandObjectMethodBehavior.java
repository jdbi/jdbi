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
package org.jdbi.v3.sqlobject;

import java.sql.Connection;
import java.sql.SQLException;

import org.jdbi.v3.core.ConnectionFactory;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

public class TestOnDemandObjectMethodBehavior {

    public interface UselessDao extends SqlObject {
        void finalize();
    }

    /**
     * Sometimes the GC will call {@link #finalize()} on a SqlObject from
     * extremely sensitive places from within the GC machinery.  Jdbi should not
     * open a {@link Connection} just to satisfy a (no-op) finalizer.
     * <a href="https://github.com/brianm/jdbi/issues/82">Issue #82</a>.
     */
    @Test
    public void testFinalizeDoesntConnect() {
        var cf = new ConnectionFactory() {
            @Override
            public Connection openConnection() throws SQLException {
                throw new AssertionError();
            }
        };
        Jdbi.create(cf)
                .installPlugin(new SqlObjectPlugin())
                .onDemand(UselessDao.class)
                .finalize();
    }
}

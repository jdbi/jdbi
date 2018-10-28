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

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Test;

public class TestOnDemandObjectMethodBehavior {
    private Jdbi db;
    private UselessDao dao;

    public interface UselessDao extends SqlObject {
        void finalize();
    }

    @Before
    public void setUp() {
        final JdbcDataSource ds = new JdbcDataSource() {
            private static final long serialVersionUID = 1L;

            @Override
            public Connection getConnection() {
                throw new UnsupportedOperationException();
            }
        };
        db = Jdbi.create(ds);
        db.installPlugin(new SqlObjectPlugin());
        dao = db.onDemand(UselessDao.class);
    }

    /**
     * Sometimes the GC will call {@link #finalize()} on a SqlObject from
     * extremely sensitive places from within the GC machinery.  Jdbi should not
     * open a {@link Connection} just to satisfy a (no-op) finalizer.
     * <a href="https://github.com/brianm/jdbi/issues/82">Issue #82</a>.
     */
    @Test
    public void testFinalizeDoesntConnect() {
        dao.finalize(); // Normally GC would do this, but just fake it
    }

}

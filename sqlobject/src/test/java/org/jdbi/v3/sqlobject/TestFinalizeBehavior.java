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

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.DBI;
import org.junit.Before;
import org.junit.Test;

/**
 * Sometimes the GC will call {@link #finalize()} on a SqlObject from
 * extremely sensitive places from within the GC machinery.  JDBI should not
 * open a {@link Connection} just to satisfy a (no-op) finalizer.
 * <a href="https://github.com/brianm/jdbi/issues/82">Issue #82</a>.
 */
public class TestFinalizeBehavior
{
    private DBI    dbi;

    interface UselessDao
    {
        public void finalize();
    }

    @Test
    public void testFinalizeDoesntConnect() throws Exception
    {
        final UselessDao dao = SqlObjectBuilder.onDemand(dbi, UselessDao.class);
        dao.finalize(); // Normally GC would do this, but just fake it
    }

    @Before
    public void setUp() throws Exception
    {
        final JdbcDataSource ds = new JdbcDataSource() {
            private static final long serialVersionUID = 1L;

            @Override
            public Connection getConnection() throws SQLException
            {
                throw new UnsupportedOperationException();
            }
        };
        dbi = new DBI(ds);
    }
}

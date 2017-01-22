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
package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

/**
 * Sometimes the GC will call {@link #finalize()} on a SqlObject from
 * extremely sensitive places from within the GC machinery.
 * <a href="https://github.com/brianm/jdbi/issues/82">Issue #82</a>.
 * Other methods also do not use a {@link Connection} and can cause issues acquiring one.
 */
public class TestObjectMethodBehavior
{
    private DBI    dbi;

    interface UselessDao
    {
        public void finalize();
    }

    @Test
    public void testFinalizeDoesntConnect() throws Exception
    {
        final UselessDao dao = dbi.onDemand(UselessDao.class);
        dao.finalize(); // Normally GC would do this, but just fake it
    }

    @Test
    public void testHashCodeDoesntConnect() throws Exception
    {
        final UselessDao dao = dbi.onDemand(UselessDao.class);
        //noinspection ResultOfMethodCallIgnored
        dao.hashCode();
    }

    @Test
    public void testEqualsDoesntConnect() throws Exception
    {
        final UselessDao dao = dbi.onDemand(UselessDao.class);
        assertEquals(dao, dao);
    }

    @Test
    public void testToStringDoesntConnect() throws Exception
    {
        final UselessDao dao = dbi.onDemand(UselessDao.class);
        //noinspection ResultOfMethodCallIgnored
        dao.toString();
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

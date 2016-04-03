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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;

import java.sql.Connection;
import java.sql.SQLException;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.DBI;
import org.junit.Before;
import org.junit.Test;

public class TestObjectMethodBehavior
{
    private DBI    dbi;
    private UselessDao dao;

    public interface UselessDao extends Cloneable
    {
        void finalize();
        Object clone();
    }

    /**
     * Sometimes the GC will call {@link #finalize()} on a SqlObject from
     * extremely sensitive places from within the GC machinery.  JDBI should not
     * open a {@link Connection} just to satisfy a (no-op) finalizer.
     * <a href="https://github.com/brianm/jdbi/issues/82">Issue #82</a>.
     */
    @Test
    public void testFinalizeDoesntConnect() throws Exception
    {
        dao.finalize(); // Normally GC would do this, but just fake it
    }

    @Test
    public void testClone() throws Exception
    {
        assertNotSame(dao, dao.clone());
    }

    @Test
    public void testEquals() throws Exception
    {
        assertEquals(dao, dao);
        assertNotEquals(dao, dao.clone());
    }

    @Test
    public void testHashCode() throws Exception
    {
        assertEquals(dao.hashCode(), dao.hashCode());
        assertNotEquals(dao.hashCode(), dao.clone().hashCode());
    }

    @Test
    public void testToStringDoesntConnect() throws Exception
    {
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
        dbi = DBI.create(ds);
        dbi.installPlugin(new SqlObjectPlugin());
        dao = dbi.onDemand(UselessDao.class);
    }
}

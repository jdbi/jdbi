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

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.util.concurrent.Callable;

import org.jdbi.v3.core.ConfigRegistry;
import org.jdbi.v3.core.ExtensionMethod;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleSupplier;
import org.jdbi.v3.sqlobject.mixins.GetHandle;
import org.junit.Before;
import org.junit.Test;

public class TestSqlObjectMethodBehavior
{
    private UselessDao dao;
    private UselessDao anotherDao;

    @Before
    public void setUp() throws Exception
    {
        HandleSupplier handleSupplier = new HandleSupplier() {
            @Override
            public ConfigRegistry getConfig() {
                return new ConfigRegistry();
            }

            @Override
            public Handle getHandle() {
                throw new UnsupportedOperationException();
            }

            @Override
            public <V> V invokeInContext(ExtensionMethod extensionMethod, ConfigRegistry config, Callable<V> task) throws Exception {
                return task.call();
            }
        };
        dao = SqlObjectFactory.INSTANCE.attach(UselessDao.class, handleSupplier);
        anotherDao = SqlObjectFactory.INSTANCE.attach(UselessDao.class, handleSupplier);
    }

    public interface UselessDao extends GetHandle
    {
        void finalize();
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
    public void testEquals() throws Exception
    {
        assertThat(dao).isEqualTo(dao);
        assertThat(dao).isNotEqualTo(anotherDao);
    }

    @Test
    public void testHashCode() throws Exception
    {
        assertThat(dao.hashCode()).isEqualTo(dao.hashCode());
        assertThat(dao.hashCode()).isNotEqualTo(anotherDao.hashCode());
    }

    @Test
    public void testToStringDoesntConnect() throws Exception
    {
        dao.toString();
    }
}

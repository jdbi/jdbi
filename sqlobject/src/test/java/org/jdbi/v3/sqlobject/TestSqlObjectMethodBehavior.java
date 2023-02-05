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
import java.util.concurrent.Callable;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionContext;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class TestSqlObjectMethodBehavior {
    private UselessDao dao;
    private UselessDao anotherDao;

    @BeforeEach
    public void setUp() {
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
            public Jdbi getJdbi() {
                throw new UnsupportedOperationException();
            }

            @Override
            public <V> V invokeInContext(ExtensionContext extensionContext, Callable<V> task) throws Exception {
                return task.call();
            }
        };
        SqlObjectFactory factory = new SqlObjectFactory();
        dao = factory.attach(UselessDao.class, handleSupplier);
        anotherDao = factory.attach(UselessDao.class, handleSupplier);
    }

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
        try {
            dao.finalize(); // Normally GC would do this, but just fake it
        } catch (UnsupportedOperationException e) {
            fail("should not open a connection");
        }
    }

    @Test
    public void testEquals() {
        assertThat(dao).isEqualTo(dao)
                .isNotEqualTo(anotherDao);
    }

    @Test
    public void testHashCode() {
        assertThat(dao).hasSameHashCodeAs(dao)
                .doesNotHaveSameHashCodeAs(anotherDao);
    }

    @Test
    public void testToStringDoesntConnect() {
        try {
            dao.toString();
        } catch (UnsupportedOperationException e) {
            fail("should not open a connection");
        }
    }

}

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
package org.jdbi.sqlobject;

import java.sql.Connection;
import java.util.concurrent.Callable;

import com.google.common.testing.EqualsTester;
import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.extension.ExtensionContext;
import org.jdbi.core.extension.ExtensionFactory;
import org.jdbi.core.extension.Extensions;
import org.jdbi.core.extension.HandleSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class TestSqlObjectMethodBehavior {

    private Jdbi jdbi;
    private UselessDao dao;
    private UselessDao anotherDao;

    @BeforeEach
    public void setUp() {
        jdbi = Jdbi.create(() -> {
            throw new UnsupportedOperationException();
        });

        jdbi.registerExtension(new SqlObjectFactory());

        // TODO - rewrite this test, it is strongly discouraged to create a handle supplier
        // manually. Once we go to Java 17, HandleSupplier will be a sealed class.
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

        ExtensionFactory factory = jdbi.getConfig(Extensions.class).findFactory(SqlObjectFactory.class)
                .orElseGet(() -> fail("Could not retrieve factory"));

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
        new EqualsTester().addEqualityGroup(dao, dao)
            .addEqualityGroup(anotherDao)
            .testEquals();
    }

    @Test
    public void testHashCode() {
        assertThat(dao).hasSameHashCodeAs(dao)
                .doesNotHaveSameHashCodeAs(anotherDao);
    }

    @Test
    public void testToStringDoesntConnect() {
        try {
            assertThat(dao.toString()).isNotNull();
        } catch (UnsupportedOperationException e) {
            fail("should not open a connection");
        }
    }

}

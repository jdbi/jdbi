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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Set;

import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jdbi.v3.core.extension.ExtensionFactory.FactoryFlag.NON_VIRTUAL_FACTORY;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TestOnDemandMethodBehavior {
    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private Connection connection;

    private Jdbi db;

    private UselessDao onDemand;

    private UselessDao anotherOnDemand;

    public interface UselessDao {
        default void run(Runnable runnable) {
            runnable.run();
        }

        default void blowUp() throws SQLException {
            throw new SQLException("boom");
        }

        Handle getHandle();

        void foo();
    }

    public class UselessDaoExtension implements ExtensionFactory {
        @Override
        public boolean accepts(Class<?> extensionType) {
            return UselessDao.class.equals(extensionType);
        }

        @Override
        public <E> E attach(Class<E> extensionType, HandleSupplier handleSupplier) {
            return extensionType.cast(new UselessDao() {
                @Override
                public Handle getHandle() {
                    return handleSupplier.getHandle();
                }

                @Override
                public void foo() {}
            });
        }

        @Override
        public Set<FactoryFlag> getFactoryFlags() {
            return EnumSet.of(NON_VIRTUAL_FACTORY);
        }
    }

    @BeforeEach
    public void setUp() {
        db = Jdbi.create(connectionFactory);
        db.registerExtension(new UselessDaoExtension());
        onDemand = db.onDemand(UselessDao.class);
        anotherOnDemand = db.onDemand(UselessDao.class);
    }

    @Test
    public void testEqualsDoesntAttach() throws SQLException {
        assertThat(onDemand).isEqualTo(onDemand)
                .isNotEqualTo(anotherOnDemand);
        verify(connectionFactory, never()).openConnection();
    }

    @Test
    public void testHashCodeDoesntAttach() throws SQLException {
        assertThat(onDemand).hasSameHashCodeAs(onDemand)
                .doesNotHaveSameHashCodeAs(anotherOnDemand);
        verify(connectionFactory, never()).openConnection();
    }

    @Test
    public void testToStringDoesntAttach() throws SQLException {
        assertThat(onDemand.toString()).isNotNull();
        verify(connectionFactory, never()).openConnection();
    }

    @Test
    public void testReentrantCallReusesHandle() throws Exception {
        when(connectionFactory.openConnection())
            .thenReturn(connection)
            .thenThrow(IllegalStateException.class);

        when(connectionFactory.getCleanableFor(connection))
            .thenReturn(() -> connection.close());

        onDemand.run(() -> assertThat(onDemand.getHandle().getConnection()).isSameAs(connection));

        verify(connectionFactory, times(1)).openConnection();
    }

    @Test
    public void testNestedCallThroughSeparateOnDemandReusesHandle() throws Exception {
        when(connectionFactory.openConnection())
            .thenReturn(connection)
            .thenThrow(IllegalStateException.class);

        when(connectionFactory.getCleanableFor(connection))
            .thenReturn(() -> connection.close());

        onDemand.run(() -> assertThat(anotherOnDemand.getHandle().getConnection()).isSameAs(connection));

        verify(connectionFactory, times(1)).openConnection();
    }

    @Test
    public void testExceptionThrown() {
        db.registerExtension(new UselessDaoExtension());
        UselessDao uselessDao = db.onDemand(UselessDao.class);
        assertThatThrownBy(uselessDao::blowUp).isInstanceOf(SQLException.class);
    }
}

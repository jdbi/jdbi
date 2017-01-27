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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TestOnDemandMethodBehavior {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ExtensionFactory mockExtensionFactory;

    @Mock
    private UselessDao mockDao;

    private Jdbi db;

    private UselessDao onDemand;

    private UselessDao anotherOnDemand;

    public interface UselessDao {
        default void run(Runnable runnable) {
            runnable.run();
        }

        void foo();
    }

    @Before
    public void setUp() throws Exception
    {
        when(mockExtensionFactory.accepts(UselessDao.class)).thenReturn(true);

        final JdbcDataSource ds = new JdbcDataSource() {
            private static final long serialVersionUID = 1L;

            @Override
            public Connection getConnection() throws SQLException
            {
                throw new UnsupportedOperationException();
            }
        };
        db = Jdbi.create(ds);
        db.registerExtension(mockExtensionFactory);
        onDemand = db.onDemand(UselessDao.class);
        anotherOnDemand = db.onDemand(UselessDao.class);
    }

    @Test
    public void testEqualsDoesntAttach() throws Exception
    {
        assertThat(onDemand).isEqualTo(onDemand);
        assertThat(onDemand).isNotEqualTo(anotherOnDemand);
        verify(mockExtensionFactory, never()).attach(any(), any());
    }

    @Test
    public void testHashCodeDoesntAttach() throws Exception
    {
        assertThat(onDemand.hashCode()).isEqualTo(onDemand.hashCode());
        assertThat(onDemand.hashCode()).isNotEqualTo(anotherOnDemand.hashCode());
        verify(mockExtensionFactory, never()).attach(any(), any());
    }

    @Test
    public void testToStringDoesntAttach() throws Exception
    {
        assertThat(onDemand.toString()).isNotNull();
        verify(mockExtensionFactory, never()).attach(any(), any());
    }

    @Test
    public void testReentrantCallReusesExtension() {
        when(mockExtensionFactory.attach(any(), any()))
                .thenReturn(mockDao)
                .thenThrow(IllegalStateException.class);

        doCallRealMethod().when(mockDao).run(any());
        onDemand.run(onDemand::foo);

        verify(mockExtensionFactory).attach(eq(UselessDao.class), any());
        verify(mockDao).run(any());
        verify(mockDao).foo();
    }
}

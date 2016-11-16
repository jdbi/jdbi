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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.ImmutableSet;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.MappingRegistry;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizers.TransactionIsolation;
import org.jdbi.v3.sqlobject.mixins.Transactional;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestDoublyTransactional
{
    private Jdbi    dbi;
    private Handle handle;
    private final AtomicBoolean inTransaction = new AtomicBoolean();

    public interface TheBasics extends Transactional<TheBasics>
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        @TransactionIsolation(TransactionIsolationLevel.SERIALIZABLE)
        int insert(@BindBean Something something);
    }

    @Test
    public void testDoublyTransactional() throws Exception
    {
        final TheBasics dao = dbi.onDemand(TheBasics.class);
        dao.inTransaction(TransactionIsolationLevel.SERIALIZABLE, (transactional, status) -> {
            transactional.insert(new Something(1, "2"));
            inTransaction.set(true);
            transactional.insert(new Something(2, "3"));
            inTransaction.set(false);
            return null;
        });
    }

    @Before
    public void setUp() throws Exception
    {
        final JdbcDataSource ds = new JdbcDataSource() {
            private static final long serialVersionUID = 1L;

            @Override
            public Connection getConnection() throws SQLException
            {
                final Connection real = super.getConnection();
                return (Connection) Proxy.newProxyInstance(real.getClass().getClassLoader(), new Class<?>[] {Connection.class}, new TxnIsolationCheckingInvocationHandler(real));
            }
        };
        // in MVCC mode h2 doesn't shut down immediately on all connections closed, so need random db name
        ds.setURL(String.format("jdbc:h2:mem:%s;MVCC=TRUE", UUID.randomUUID()));
        dbi = Jdbi.create(ds);
        dbi.installPlugin(new SqlObjectPlugin());
        dbi.getConfig(MappingRegistry.class).registerRowMapper(new SomethingMapper());

        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }

    private static final Set<Method> CHECKED_METHODS;
    static {
        try {
            CHECKED_METHODS = ImmutableSet.of(Connection.class.getMethod("setTransactionIsolation", int.class));
        } catch (final Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private class TxnIsolationCheckingInvocationHandler implements InvocationHandler
    {
        private final Connection real;

        public TxnIsolationCheckingInvocationHandler(Connection real)
        {
            this.real = real;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            if (CHECKED_METHODS.contains(method) && inTransaction.get()) {
                throw new SQLException("PostgreSQL would not let you set the transaction isolation here");
            }
            return method.invoke(real, args);
        }
    }
}

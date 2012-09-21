package org.skife.jdbi.v2.sqlobject;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.sqlobject.customizers.TransactionIsolation;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.ImmutableSet;

public class TestDoublyTransactional extends TestCase
{
    private DBI    dbi;
    private Handle handle;
    private final AtomicBoolean inTransaction = new AtomicBoolean();

    interface TheBasics extends Transactional<TheBasics>
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        @TransactionIsolation(TransactionIsolationLevel.SERIALIZABLE)
        int insert(@BindBean Something something);
    }

    @Test
    public void testDoublyTransactional() throws Exception
    {
        final TheBasics dao = dbi.onDemand(TheBasics.class);
        dao.inTransaction(TransactionIsolationLevel.SERIALIZABLE, new Transaction<Void, TheBasics>() {
            @Override
            public Void inTransaction(TheBasics transactional, TransactionStatus status) throws Exception
            {
                transactional.insert(new Something(1, "2"));
                inTransaction.set(true);
                transactional.insert(new Something(2, "3"));
                inTransaction.set(false);
                return null;
            }
        });
    }

    @Override
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
        ds.setURL("jdbc:h2:mem:test" + new Random().nextInt() + ";MVCC=TRUE");

        dbi = new DBI(ds);

        dbi.registerMapper(new SomethingMapper());

        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");
    }

    @Override
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
                throw new SQLException(String.format("PostgreSQL would not let you set the transaction isolation here"));
            }
            return method.invoke(real, args);
        }
    }
}

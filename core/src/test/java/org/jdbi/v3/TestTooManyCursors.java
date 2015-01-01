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
package org.jdbi.v3;

import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.jdbi.v3.exceptions.CallbackFailedException;
import org.jdbi.v3.tweak.HandleCallback;
import org.junit.Rule;
import org.junit.Test;

/**
 * Oracle was getting angry about too many open cursors because of the large number
 * of prepared statements being created and cached indefinitely.
 */
public class TestTooManyCursors
{
    @Rule
    public MemoryDatabase db = new MemoryDatabase();

    @Test
    public void testFoo() throws Exception
    {
        DataSource ds = db.getDataSource();
        DataSource dataSource = new ErrorProducingDataSource(ds, 99);
        DBI dbi = new DBI(dataSource);

        try {
            dbi.withHandle(new HandleCallback<Object>()
            {
                @Override
                public Void withHandle(Handle handle) throws Exception
                {
                    handle.setStatementBuilder(new DefaultStatementBuilder());
                    for (int idx = 0; idx < 100; idx++) {
                        handle.createQuery("SELECT " + idx + " FROM something").first();
                    }
                    return null;
                }
            });
        }
        catch (CallbackFailedException e) {
            fail("We have too many open connections");
        }
    }

    private static class ErrorProducingDataSource implements DataSource
    {
        private final DataSource target;
        private final int connCount;

        ErrorProducingDataSource(DataSource target, int i)
        {
            this.target = target;
            connCount = i;
        }

        @Override
        public Connection getConnection() throws SQLException
        {
            return ConnectionInvocationHandler.newInstance(target.getConnection(), connCount);
        }

        @Override
        public Connection getConnection(String string, String string1) throws SQLException
        {
            return ConnectionInvocationHandler.newInstance(target.getConnection(string, string1), connCount);
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException
        {
            return target.getLogWriter();
        }

        @Override
        public void setLogWriter(PrintWriter printWriter) throws SQLException
        {
            target.setLogWriter(printWriter);
        }

        @Override
        public void setLoginTimeout(int i) throws SQLException
        {
            target.setLoginTimeout(i);
        }

        @Override
        public int getLoginTimeout() throws SQLException
        {
            return target.getLoginTimeout();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException
        {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException
        {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException
        {
            throw new UnsupportedOperationException();
        }
    }


    private static class ConnectionInvocationHandler implements InvocationHandler
    {
        private final Connection connection;
        private final int numSuccessfulStatements;
        private int numStatements = 0;

        public static Connection newInstance(Connection connection, int numSuccessfulStatements)
        {
            return (Connection) Proxy.newProxyInstance(connection.getClass().getClassLoader(),
                                                       new Class[]{Connection.class},
                                                       new ConnectionInvocationHandler(connection, numSuccessfulStatements));
        }

        public ConnectionInvocationHandler(Connection connection, int numSuccessfulStatements)
        {
            this.connection = connection;
            this.numSuccessfulStatements = numSuccessfulStatements;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            try {
                if ("createStatement".equals(method.getName()) ||
                    "prepareCall".equals(method.getName()) ||
                    "prepareStatement".equals(method.getName())) {
                    if (++numStatements > numSuccessfulStatements) {
                        throw new SQLException("Fake 'maximum open cursors exceeded' error");
                    }
                    return StatementInvocationHandler.newInstance((Statement) method.invoke(connection, args), this);
                }
                else {
                    return method.invoke(connection, args);
                }
            }
            catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }

        public void registerCloseStatement()
        {
            numStatements--;
        }
    }

    private static class StatementInvocationHandler implements InvocationHandler
    {
        private final Statement stmt;
        private final ConnectionInvocationHandler connectionHandler;

        public static Statement newInstance(Statement stmt, ConnectionInvocationHandler connectionHandler)
        {

            Class<?> o = stmt.getClass();
            List<Class<?>> interfaces = new ArrayList<Class<?>>();
            while (!o.equals(Object.class)) {
                interfaces.addAll(Arrays.asList(o.getInterfaces()));
                o = o.getSuperclass();
            }

            return (Statement) Proxy.newProxyInstance(stmt.getClass().getClassLoader(),
                                                      interfaces.toArray(new Class[interfaces.size()]),
                                                      new StatementInvocationHandler(stmt, connectionHandler));
        }

        public StatementInvocationHandler(Statement stmt, ConnectionInvocationHandler connectionHandler)
        {
            this.stmt = stmt;
            this.connectionHandler = connectionHandler;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            if ("close".equals(method.getName())) {
                connectionHandler.registerCloseStatement();
            }
            try {
                return method.invoke(stmt, args);
            }
            catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }
}

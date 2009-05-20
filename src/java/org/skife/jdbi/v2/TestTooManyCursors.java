package org.skife.jdbi.v2;

import org.skife.jdbi.derby.Tools;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.tweak.HandleCallback;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Oracle was getting angry about too many open cursors because of the large number
 * of prepared statements being created and cached indefinitely.
 */
public class TestTooManyCursors extends DBITestCase
{
    public void testFoo() throws Exception
    {
        DataSource ds = Tools.getDataSource();
        DataSource dataSource = new ErrorProducingDataSource(ds, 99);
        IDBI dbi = new DBI(dataSource);

        try {
            dbi.withHandle(new HandleCallback<Object>()
            {
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

        public Connection getConnection() throws SQLException
        {
            return ConnectionInvocationHandler.newInstance(target.getConnection(), connCount);
        }

        public Connection getConnection(String string, String string1) throws SQLException
        {
            return ConnectionInvocationHandler.newInstance(target.getConnection(string, string1), connCount);
        }

        public PrintWriter getLogWriter() throws SQLException
        {
            return target.getLogWriter();
        }

        public void setLogWriter(PrintWriter printWriter) throws SQLException
        {
            target.setLogWriter(printWriter);
        }

        public void setLoginTimeout(int i) throws SQLException
        {
            target.setLoginTimeout(i);
        }

        public int getLoginTimeout() throws SQLException
        {
            return target.getLoginTimeout();
        }

	    public <T> T unwrap(Class<T> iface) throws SQLException
	    {
		    return null;
	    }

	    public boolean isWrapperFor(Class<?> iface) throws SQLException
	    {
		    return false;
	    }
    }


    private static class ConnectionInvocationHandler implements InvocationHandler
    {
        private Connection connection;
        private int numSuccessfulStatements;
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
        private Statement stmt;
        private ConnectionInvocationHandler connectionHandler;

        public static Statement newInstance(Statement stmt, ConnectionInvocationHandler connectionHandler)
        {

            Class o = stmt.getClass();
            List<Class> interfaces = new ArrayList<Class>();
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
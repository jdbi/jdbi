/*
 * Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;
import org.skife.jdbi.v2.tweak.ConnectionFactory;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.StatementBuilderFactory;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.TransactionHandler;
import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.tweak.transactions.LocalTransactionHandler;
import org.skife.jdbi.v2.logging.NoOpLog;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class  provides the access point for jDBI. Use it to obtain Handle instances
 * and provide "global" configuration for all handles obtained from it.
 */
public class DBI implements IDBI
{
    private final ConnectionFactory connectionFactory;
    private StatementRewriter statementRewriter = new ColonPrefixNamedParamStatementRewriter();
    private StatementLocator statementLocator = new ClasspathStatementLocator();
    private TransactionHandler transactionhandler = new LocalTransactionHandler();
    private StatementBuilderFactory statementBuilderFactory = new DefaultStatementBuilderFactory();
    private final Map<String, Object> globalStatementAttributes = new ConcurrentHashMap<String, Object>();
    private SQLLog log = new NoOpLog();

    /**
     * Constructor for use with a DataSource which will provide
     *
     * @param dataSource
     */
    public DBI(DataSource dataSource)
    {
        this(new DataSourceConnectionFactory(dataSource));
        assert (dataSource != null);
    }

    /**
     * Constructor used to allow for obtaining a Connection in a customized manner.
     * <p/>
     * The {@link org.skife.jdbi.v2.tweak.ConnectionFactory#openConnection()} method will
     * be invoked to obtain a connection instance whenever a Handle is opened.
     *
     * @param connectionFactory PrvidesJDBC connections to Handle instances
     */
    public DBI(ConnectionFactory connectionFactory)
    {
        assert (connectionFactory != null);
        this.connectionFactory = connectionFactory;
    }

    /**
     * Create a DBI which directly uses the DriverManager
     *
     * @param url JDBC URL for connections
     */
    public DBI(final String url)
    {
        this(new ConnectionFactory()
        {
            public Connection openConnection() throws SQLException
            {
                return DriverManager.getConnection(url);
            }
        });
    }

    /**
     * Create a DBI which directly uses the DriverManager
     *
     * @param url   JDBC URL for connections
     * @param props Properties to pass to DriverManager.getConnection(url, props) for each new handle
     */
    public DBI(final String url, final Properties props)
    {
        this(new ConnectionFactory()
        {
            public Connection openConnection() throws SQLException
            {
                return DriverManager.getConnection(url, props);
            }
        });
    }

    /**
     * Create a DBI which directly uses the DriverManager
     *
     * @param url      JDBC URL for connections
     * @param username User name for connection authentication
     * @param password Password for connection authentication
     */
    public DBI(final String url, final String username, final String password)
    {
        this(new ConnectionFactory()
        {
            public Connection openConnection() throws SQLException
            {
                return DriverManager.getConnection(url, username, password);
            }
        });
    }

    /**
     * Use a non-standard StatementLocator to look up named statements for all
     * handles created from this DBi instance.
     *
     * @param locator StatementLocator which will be used by all Handle instances
     *                created from this DBI
     */
    public void setStatementLocator(StatementLocator locator)
    {
        assert (locator != null);
        this.statementLocator = locator;
    }

    /**
     * Use a non-standard StatementRewriter to transform SQL for all Handle instances
     * created by this DBI.
     *
     * @param rewriter StatementRewriter to use on all Handle instances
     */
    public void setStatementRewriter(StatementRewriter rewriter)
    {
        assert (rewriter != null);
        this.statementRewriter = rewriter;
    }

    /**
     * Specify the TransactionHandler instance to use. This allows overriding
     * transaction semantics, or mapping into different transaction
     * management systems.
     * <p/>
     * The default version uses local transactions on the database Connection
     * instances obtained.
     *
     * @param handler The TransactionHandler to use for all Handle instances obtained
     *                from this DBI
     */
    public void setTransactionHandler(TransactionHandler handler)
    {
        assert (handler != null);
        this.transactionhandler = handler;
    }

    /**
     * Obtain a Handle to the data source wrapped by this DBI instance
     *
     * @return an open Handle instance
     */
    public Handle open()
    {
        try {
            Connection conn = connectionFactory.openConnection();
            StatementBuilder cache = statementBuilderFactory.createStatementBuilder(conn);
            return new BasicHandle(transactionhandler,
                                   statementLocator,
                                   cache,
                                   statementRewriter,
                                   conn,
                                   globalStatementAttributes,
                                   log);
        }
        catch (SQLException e) {
            throw new UnableToObtainConnectionException(e);
        }
    }

    /**
     * Define an attribute on every {@link StatementContext} for every statement created
     * from a handle obtained from this DBI instance.
     *
     * @param key The key for the attribute
     * @param value the value for the attribute
     */
    public void define(String key, Object value)
    {
        this.globalStatementAttributes.put(key, value);
    }

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients.
     *
     * @param callback A callback which will receive an open Handle
     * @return the value returned by callback
     * @throws CallbackFailedException Will be thrown if callback raises an exception. This exception will
     *                                 wrap the exception thrown by the callback.
     */
    public <ReturnType> ReturnType withHandle(HandleCallback<ReturnType> callback) throws CallbackFailedException
    {
        final Handle h = this.open();
        try {
            return callback.withHandle(h);
        }
        catch (Exception e) {
            throw new CallbackFailedException(e);
        }
        finally {
            h.close();
        }
    }

    /**
     * Convenience methd used to obtain a handle from a specific data source
     *
     * @param dataSource
     *
     * @return Handle using a Connection obtained from the provided DataSource
     */
    public static Handle open(DataSource dataSource)
    {
        assert (dataSource != null);
        return new DBI(dataSource).open();
    }

    /**
     * Create a Handle wrapping a particular JDBC Connection
     *
     * @param connection
     *
     * @return Handle bound to connection
     */
    public static Handle open(final Connection connection)
    {
        assert (connection != null);
        return new DBI(new ConnectionFactory()
        {
            public Connection openConnection()
            {
                return connection;
            }
        }).open();
    }

    /**
     * Obtain a handle with just a JDBC URL
     *
     * @param url JDBC Url
     *
     * @return newly opened Handle
     */
    public static Handle open(final String url)
    {
        assert (url != null);
        return new DBI(url).open();
    }

    /**
     * Obtain a handle with just a JDBC URL
     *
     * @param url      JDBC Url
     * @param username JDBC username for authentication
     * @param password JDBC password for authentication
     *
     * @return newly opened Handle
     */
    public static Handle open(final String url, final String username, final String password)
    {
        assert (url != null);
        return new DBI(url, username, password).open();
    }

    /**
     * Obtain a handle with just a JDBC URL
     *
     * @param url   JDBC Url
     * @param props JDBC properties
     *
     * @return newly opened Handle
     */
    public static Handle open(final String url, final Properties props)
    {
        assert (url != null);
        return new DBI(url, props).open();
    }

    /**
     * Allows customization of how prepared statements are created. When a Handle is created
     * against this DBI instance the factory will be used to create a StatementBuilder for
     * that specific handle. When the handle is closed, the StatementBuilder's close method
     * will be invoked.
     */
    public void setStatementBuilderFactory(StatementBuilderFactory factory)
    {
        this.statementBuilderFactory = factory;
    }

    /**
     * Specify the class used to log sql statements. Will be passed to all handles created from
     * this instance
     */
    public void setSQLLog(SQLLog log)
    {
        this.log = log;
    }
}

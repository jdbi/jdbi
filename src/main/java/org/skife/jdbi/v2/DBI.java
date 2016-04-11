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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;
import org.skife.jdbi.v2.logging.NoOpLog;
import org.skife.jdbi.v2.sqlobject.SqlObjectBuilder;
import org.skife.jdbi.v2.tweak.ArgumentFactory;
import org.skife.jdbi.v2.tweak.ConnectionFactory;
import org.skife.jdbi.v2.tweak.ContainerFactory;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.HandleConsumer;
import org.skife.jdbi.v2.tweak.ResultColumnMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.StatementBuilderFactory;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.TransactionHandler;
import org.skife.jdbi.v2.tweak.VoidHandleCallback;
import org.skife.jdbi.v2.tweak.transactions.LocalTransactionHandler;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class  provides the access point for jDBI. Use it to obtain Handle instances
 * and provide "global" configuration for all handles obtained from it.
 */
public class DBI implements IDBI
{
    private final Map<String, Object> globalStatementAttributes = new ConcurrentHashMap<String, Object>();
    private final MappingRegistry mappingRegistry = new MappingRegistry();
    private final ContainerFactoryRegistry containerFactoryRegistry = new ContainerFactoryRegistry();
    private final Foreman foreman = new Foreman();

    private final ConnectionFactory connectionFactory;

    private AtomicReference<StatementRewriter> statementRewriter = new AtomicReference<StatementRewriter>(new ColonPrefixNamedParamStatementRewriter());
    private AtomicReference<StatementLocator> statementLocator = new AtomicReference<StatementLocator>(new ClasspathStatementLocator());
    private AtomicReference<TransactionHandler> transactionhandler = new AtomicReference<TransactionHandler>(new LocalTransactionHandler());
    private AtomicReference<StatementBuilderFactory> statementBuilderFactory = new AtomicReference<StatementBuilderFactory>(new DefaultStatementBuilderFactory());
    private AtomicReference<SQLLog> log = new AtomicReference<SQLLog>(new NoOpLog());
    private AtomicReference<TimingCollector> timingCollector = new AtomicReference<TimingCollector>(TimingCollector.NOP_TIMING_COLLECTOR);

    /**
     * Constructor for use with a DataSource which will provide
     *
     * @param dataSource
     */
    public DBI(DataSource dataSource)
    {
        this(new DataSourceConnectionFactory(dataSource));
        assert dataSource != null;
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
        assert connectionFactory != null;
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
            @Override
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
            @Override
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
            @Override
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
        assert locator != null;
        this.statementLocator.set(locator);
    }

    public StatementLocator getStatementLocator()
    {
        return this.statementLocator.get();
    }

    /**
     * Use a non-standard StatementRewriter to transform SQL for all Handle instances
     * created by this DBI.
     *
     * @param rewriter StatementRewriter to use on all Handle instances
     */
    public void setStatementRewriter(StatementRewriter rewriter)
    {
        assert rewriter != null;
        this.statementRewriter.set(rewriter);
    }

    public StatementRewriter getStatementRewriter()
    {
        return this.statementRewriter.get();
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
        assert handler != null;
        this.transactionhandler.set(handler);
    }

    public TransactionHandler getTransactionHandler()
    {
        return this.transactionhandler.get();
    }

    /**
     * Obtain a Handle to the data source wrapped by this DBI instance
     *
     * @return an open Handle instance
     */
    @Override
    public Handle open()
    {
        try {
            final long start = System.nanoTime();
            Connection conn = connectionFactory.openConnection();
            final long stop = System.nanoTime();
            StatementBuilder cache = statementBuilderFactory.get().createStatementBuilder(conn);
            Handle h = new BasicHandle(transactionhandler.get(),
                                       statementLocator.get(),
                                       cache,
                                       statementRewriter.get(),
                                       conn,
                                       globalStatementAttributes,
                                       log.get(),
                                       timingCollector.get(),
                                       new MappingRegistry(mappingRegistry),
                                       foreman.createChild(),
                                       containerFactoryRegistry.createChild());
            log.get().logObtainHandle((stop - start) / 1000000L, h);
            return h;
        }
        catch (SQLException e) {
            throw new UnableToObtainConnectionException(e);
        }
    }

    /**
     * Register a result set mapper which will have its parameterized type inspected to determine what it maps to
     *
     * Will be used with {@link Query#mapTo(Class)} for registered mappings.
     */
    public void registerMapper(ResultSetMapper mapper) {
        mappingRegistry.addMapper(mapper);
    }

    /**
     * Register a result set mapper factory.
     *
     * Will be used with {@link Query#mapTo(Class)} for registered mappings.
     */
    public void registerMapper(ResultSetMapperFactory factory) {
        mappingRegistry.addMapper(factory);
    }

    /**
     * Register a result column mapper which will have its parameterized type inspected to determine what it maps to
     *
     * Column mappers may be reused by {@link ResultSetMapper} to map individual columns.
     */
    public void registerColumnMapper(ResultColumnMapper mapper) {
        mappingRegistry.addColumnMapper(mapper);
    }

    /**
     * Register a result column mapper factory.
     *
     * Column mappers may be reused by {@link ResultSetMapper} to map individual columns.
     */
    public void registerColumnMapper(ResultColumnMapperFactory factory) {
        mappingRegistry.addColumnMapper(factory);
    }

    /**
     * Define an attribute on every {@link StatementContext} for every statement created
     * from a handle obtained from this DBI instance.
     *
     * @param key   The key for the attribute
     * @param value the value for the attribute
     */
    @Override
    public void define(String key, Object value)
    {
        this.globalStatementAttributes.put(key, value);
    }

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients.
     *
     * @param callback A callback which will receive an open Handle
     *
     * @return the value returned by callback
     *
     * @throws CallbackFailedException Will be thrown if callback raises an exception. This exception will
     *                                 wrap the exception thrown by the callback.
     */
    @Override
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
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients.
     *
     * @param callback A callback which will receive an open Handle
     *
     * @return the value returned by callback
     *
     * @throws CallbackFailedException Will be thrown if callback raises an exception. This exception will
     *                                 wrap the exception thrown by the callback.
     */
    @Override
    public void useHandle(final HandleConsumer callback) throws CallbackFailedException
    {
        withHandle(new VoidHandleCallback() {
            @Override
            protected void execute(Handle handle) throws Exception {
                callback.useHandle(handle);
            }
        });
    }

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients. The handle will be in a transaction when the callback is invoked, and
     * that transaction will be committed if the callback finishes normally, or rolled back if the
     * callback raises an exception.
     *
     * @param callback A callback which will receive an open Handle, in a transaction
     *
     * @return the value returned by callback
     *
     * @throws CallbackFailedException Will be thrown if callback raises an exception. This exception will
     *                                 wrap the exception thrown by the callback.
     */
    @Override
    public <ReturnType> ReturnType inTransaction(final TransactionCallback<ReturnType> callback) throws CallbackFailedException
    {
        return withHandle(new HandleCallback<ReturnType>() {
            @Override
            public ReturnType withHandle(Handle handle) throws Exception
            {
                return handle.inTransaction(callback);
            }
        });
    }

    @Override
    public void useTransaction(final TransactionConsumer callback) throws CallbackFailedException
    {
        useHandle(new HandleConsumer() {
            @Override
            public void useHandle(Handle handle) throws Exception
            {
                handle.useTransaction(callback);
            }
        });
    }

    @Override
    public <ReturnType> ReturnType inTransaction(final TransactionIsolationLevel isolation, final TransactionCallback<ReturnType> callback) throws CallbackFailedException
    {
        return withHandle(new HandleCallback<ReturnType>() {
            @Override
            public ReturnType withHandle(Handle handle) throws Exception
            {
                return handle.inTransaction(isolation, callback);
            }
        });
    }

    @Override
    public void useTransaction(final TransactionIsolationLevel isolation, final TransactionConsumer callback) throws CallbackFailedException
    {
        useHandle(new HandleConsumer() {
            @Override
            public void useHandle(Handle handle) throws Exception
            {
                handle.useTransaction(isolation, callback);
            }
        });
    }

    /**
     * Open a handle and attach a new sql object of the specified type to that handle. Be sure to close the
     * sql object (via a close() method, or calling {@link IDBI#close(Object)}
     * @param sqlObjectType an interface with annotations declaring desired behavior
     * @param <SqlObjectType>
     * @return a new sql object of the specified type, with a dedicated handle
     */
    @Override
    public <SqlObjectType> SqlObjectType open(Class<SqlObjectType> sqlObjectType)
    {
        return SqlObjectBuilder.open(this, sqlObjectType);
    }

    /**
     * Create a new sql object which will obtain and release connections from this dbi instance, as it needs to,
     * and can, respectively. You should not explicitely close this sql object
     *
     * @param sqlObjectType an interface with annotations declaring desired behavior
     * @param <SqlObjectType>
     * @return a new sql object of the specified type, with a dedicated handle
     */
    @Override
    public <SqlObjectType> SqlObjectType onDemand(Class<SqlObjectType> sqlObjectType)
    {
        return SqlObjectBuilder.onDemand(this, sqlObjectType);
    }

    /**
     * Used to close a sql object which lacks a close() method.
     * @param sqlObject the sql object to close
     */
    @Override
    public void close(Object sqlObject)
    {
        if (sqlObject instanceof Handle) {
            // just because someone is *sure* to do it
            Handle h = (Handle) sqlObject;
            h.close();
        }
        else {
            SqlObjectBuilder.close(sqlObject);
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
        assert dataSource != null;
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
        assert connection != null;
        return new DBI(new ConnectionFactory()
        {
            @Override
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
        assert url != null;
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
        assert url != null;
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
        assert url != null;
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
        this.statementBuilderFactory.set(factory);
    }

    public StatementBuilderFactory getStatementBuilderFactory()
    {
        return this.statementBuilderFactory.get();
    }

    /**
     * Specify the class used to log sql statements. Will be passed to all handles created from
     * this instance
     */
    public void setSQLLog(SQLLog log)
    {
        this.log.set(log);
    }

    public SQLLog getSQLLog()
    {
        return this.log.get();
    }

    /**
     * Add a callback to accumulate timing information about the queries running from this
     * data source.
     */
    public void setTimingCollector(final TimingCollector timingCollector) {
        if (timingCollector == null) {
            this.timingCollector.set(TimingCollector.NOP_TIMING_COLLECTOR);
        }
        else {
            this.timingCollector.set(timingCollector);
        }
    }

    public TimingCollector getTimingCollector()
    {
        return this.timingCollector.get();
    }

    public void registerArgumentFactory(ArgumentFactory<?> argumentFactory)
    {
        foreman.register(argumentFactory);
    }

    public void registerContainerFactory(ContainerFactory<?> factory)
    {
        this.containerFactoryRegistry.register(factory);
    }
}

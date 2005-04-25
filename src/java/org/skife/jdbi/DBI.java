/* Copyright 2004-2005 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi;

import org.skife.jdbi.tweak.ConnectionTransactionHandler;
import org.skife.jdbi.tweak.StatementLocator;
import org.skife.jdbi.tweak.TransactionHandler;
import org.skife.jdbi.unstable.decorator.HandleDecorator;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Entry point for operations. May act as a configured bean, or provide handles
 * via static functions.
 */
public class DBI implements IDBI
{
    private final NamedStatementRepository repository;
    private final ConnectionFactory factory;
    private HandleDecorator handleDecorator = new NullHandleDecorator();
    private TransactionHandler transactionHandler = new ConnectionTransactionHandler();
    private Map globals = new HashMap();

    /**
     * Attempt to auto-configure a DBi instance
     * <p/>
     * It first looks for an <code>org.skife.jdbi.properties-file</code> system property which
     * represents a properties file to be loaded via the classpath. If that is not found, it looks
     * for <code>jdbi.properties</code>, then <code>jdbc.properties</code>, then
     * <code>dbi.properties</code>, then finally <code>database.properties</code> on the classpath.
     * It will use the first it finds and stop looking for others once it finds one.
     * <p/>
     * Once a suitable configuration properties file has been loaded, jDBI will look for various
     * properties used to configure it. There are multiple possible values for each logical
     * property, and the order of preference if it should find multiple is the order listed here:
     * </p>
     * <table>
     * <tr>
     * <td>
     * <ul>
     * <li>jdbi.url</li>
     * <li>jdbc.url</li>
     * <li>connection.string</li>
     * </ul>
     * </td>
     * <td>
     * JDBC Connection URL, ie <code>jdbc:derby:my_database</code>,
     * required
     * </td>
     * </tr>
     * <tr>
     * <td>
     * <ul>
     * <li>jdbi.driver</li>
     * <li>jdbc.driver</li>
     * <li>driver</li>
     * <li>drive</li>
     * </ul>
     * </td>
     * <td>
     * JDBC Driver class name, ie <code>org.apache.derby.jdbc.EmbeddedDriver</code>,
     * required
     * </td>
     * </tr>
     * <tr>
     * <td>
     * <ul>
     * <li>jdbi.username</li>
     * <li>jdbi.user</li>
     * <li>jdbc.username</li>
     * <li>jdbc.user</li>
     * <li>username</li>
     * <li>user</li>
     * </ul>
     * </td>
     * <td>
     * Username to be used when obtaining connections from the database, optional
     * </td>
     * </tr>
     * <tr>
     * <td>
     * <ul>
     * <li>jdbi.password</li>
     * <li>jdbi.pass</li>
     * <li>jdbc.password</li>
     * <li>jdbc.pass</li>
     * <li>password</li>
     * <li>pass</li>
     * </ul>
     * </td>
     * <td>
     * Password to be used when obtaining connections from the database, optional
     * </td>
     * </tr>
     * <tr>
     * <td>
     * <ul>
     * <li>jdbi.handle-decorator-builder</li>
     * <li>jdbc.handle-decorator-builder</li>
     * <li>handle-decorator-builder</li>
     * </ul>
     * </td>
     * <td>
     * <b>Unstable Feature</b> class name of a <code>HandleDecorator</code>
     * to be used to decorate <code>Handle</code> instances obtained from the
     * <code>DBI</code> instance instantiated. This feature is functionally stable,
     * but the specific api may change somewhat while it remains offically unstable.
     * Please read the notes regarding the <code>org.skife.jdbi.unstable</code> package
     * before using this. Optional.
     * </td>
     * </tr>
     * <tr>
     * <td>
     * <ul>
     * <li>jdbi.transaction-handler</li>
     * <li>jdbc.transaction-handler</li>
     * <li>transaction-handler</li>
     * </ul>
     * </td>
     * <td>
     * <b>OPTIONAL</b> class name of a <code>TransactionHandler</code> which should
     * be used to override default transaction handling. Must supply a no-arg constructor. Optional.
     * </td>
     * </tr>
     * <tr>
     * <td>
     * <ul>
     * <li>jdbi.statement-locator</li>
     * <li>jdbc.statement-locator</li>
     * <li>statement-locator</li>
     * </ul>
     * </td>
     * <td>
     * <b>OPTIONAL</b> class name of a <code>StatementLocator</code> which should
     * be used to override default (cached from classpath) external sql statement locating. Optional.
     * </td>
     * </tr>
     * </table>
     *
     * @throws IOException on error loading jdbi.properties
     * @throws DBIError    if the properties file exists but is invalid
     */
    public DBI() throws IOException
    {
        repository = new NamedStatementRepository();
        final AutoConfigurator auto = new AutoConfigurator();
        factory = auto.getConnectionFactory();
        try
        {
            final HandleDecorator d = auto.getHandleDecoratorBuilder();
            if (d != null)
            {
                handleDecorator = d;
            }
            final TransactionHandler h = auto.getTransactionHandler();
            if (h != null)
            {
                transactionHandler = h;
            }
            final StatementLocator l = auto.getStatementLocator();
            if (l != null)
            {
                repository.setLocator(l);
            }
        }
        catch (Exception e)
        {
            throw new DBIError("Unable to instantiate handle decorator builder :" + e.getMessage(), e);
        }
    }

    /**
     * If <code>wombat</code> begins "jdbc:" the string will be treated as a jdbc driver
     * otherwise it will be used as the key for a jndi lookup to findInternal a <code>DataSrouce</code>.
     * If neither works, will throw a <code>DBIError</code>
     *
     * @param wombat jdbc connection string or jndi lookup
     * @throws DBIError if anything untoward happens.
     */
    public DBI(final String wombat)
    {
        repository = new NamedStatementRepository();
        if (wombat.startsWith("jdbc:"))
        {
            factory = new ConnectionFactory()
            {
                public Connection getConnection() throws SQLException
                {
                    return DriverManager.getConnection(wombat);
                }
            };
        }
        else
        {
            try
            {
                final InitialContext ctx = new InitialContext();
                final DataSource source = (DataSource) ctx.lookup(wombat);
                this.factory = new ConnectionFactory()
                {
                    public Connection getConnection() throws SQLException
                    {
                        return source.getConnection();
                    }
                };
            }
            catch (Exception e)
            {
                throw new DBIError("connection string is not a jdbc connection string, not a valid " +
                                   "jndi lookup wombat to a DataSource instance: [" + wombat + "]");
            }
        }
    }

    /**
     * If <code>wombat</code> begins "jdbc:" the string will be treated as a jdbc driver
     * otherwise it will be used as the key for a jndi lookup to findInternal a <code>DataSrouce</code>.
     * If neither works, will throw a <code>DBIError</code>
     *
     * @param wombat jdbc connection string or jndi lookup
     * @param name   username for grabbing connections
     * @param pass   password for grabbing connections
     * @throws DBIError if anything untoward happens.
     */
    public DBI(final String wombat, final String name, final String pass)
    {
        repository = new NamedStatementRepository();
        if (wombat.startsWith("jdbc:"))
        {
            factory = new ConnectionFactory()
            {
                public Connection getConnection() throws SQLException
                {
                    return DriverManager.getConnection(wombat, name, pass);
                }
            };
        }
        else
        {
            try
            {
                final InitialContext ctx = new InitialContext();
                final DataSource source = (DataSource) ctx.lookup(wombat);
                this.factory = new ConnectionFactory()
                {
                    public Connection getConnection() throws SQLException
                    {
                        return source.getConnection(name, pass);
                    }
                };
            }
            catch (Exception e)
            {
                throw new DBIError("connection string is not a jdbc connection string, not a valid " +
                                   "jndi lookup wombat to a DataSource instance: [" + wombat + "]");
            }
        }
    }

    /**
     * Obtain a new DBI instance
     *
     * @param source DataSource provided by client
     */
    public DBI(final DataSource source)
    {
        this(new ConnectionFactory()
        {
            public Connection getConnection() throws SQLException
            {
                return source.getConnection();
            }
        });
    }

    /**
     * Obtain a new DBI instance
     *
     * @param source DataSource provided by client
     * @param name   jdbc username
     * @param pass   jdbc user password
     */
    public DBI(final DataSource source, final String name, final String pass)
    {
        this(new ConnectionFactory()
        {
            public Connection getConnection() throws SQLException
            {
                return source.getConnection(name, pass);
            }
        });
    }

    /**
     * Use a custom implementation of <code>ConnectionFactory</code> to obtain
     * JDBC connections for handles created by this DBI
     */
    public DBI(ConnectionFactory factory)
    {
        this.repository = new NamedStatementRepository();
        this.factory = factory;
    }

    /**
     * Obtain a map containing globally set named parameter values. All handles obtained
     * from this DBI instance will use these named parameters.
     * <p>
     * Named parameters added to a handle will not be added to the DBI globals, and DBI globals added
     * after a handle is opened will not be added to the already open handles.
     */
    public Map getGlobalParameters()
    {
        return globals;
    }

    /* Operational methods */

    /**
     * Obtain a new Handle instance
     *
     * @return an open Handle
     * @throws DBIException
     */
    public Handle open() throws DBIException
    {
        try
        {
            return handleDecorator.decorate(this, new ConnectionHandle(factory.getConnection(),
                                                                       repository,
                                                                       transactionHandler,
                                                                       globals));
        }
        catch (SQLException e)
        {
            throw new DBIException("Unable to obtain JDBC Connection: " + e.getMessage(), e);
        }
    }

    /**
     * Execute the callback with an open handle, closing, and cleaning up resources,
     * after the callback exits or excepts
     *
     * @throws DBIException if exception is thrown from the callback, or
     *                      an exception occurs with the database
     * @throws DBIError     if an Error is thrown from the callback
     */
    public void open(HandleCallback callback) throws DBIException
    {
        Handle handle = null;
        try
        {
            handle = this.open();
            callback.withHandle(handle);
        }
        catch (DBIException e)
        {
            if (handle != null)
            {
                if (handle.isInTransaction())
                {
                    handle.rollback();
                }
            }
            throw e;
        }
        catch (Exception e)
        {
            if (handle != null)
            {
                if (handle.isInTransaction())
                {
                    handle.rollback();
                }
            }
            throw new DBIException("Exception thrown from callback, see nested exception", e);
        }
        catch (Error e)
        {
            if (handle != null)
            {
                if (handle.isInTransaction())
                {
                    handle.rollback();
                }
            }
            throw new DBIError("Error thrown from callback, see wrapped error", e);
        }
        finally
        {
            if (handle != null)
            {
                if (handle.isOpen())
                {
                    if (handle.isInTransaction())
                    {
                        handle.commit();
                    }
                    handle.close();
                }
            }
        }
    }

    /**
     * Obtain an unmodifiable map of all the named statements known to this
     * DBI instance.
     */
    public Map getNamedStatements()
    {
        return Collections.unmodifiableMap(new HashMap(repository.getStore()));
    }

    /**
     * Prepared a named sql statement
     *
     * @param name      name to issue query under
     * @param statement sql string to use as query
     * @throws DBIException if there is a problem preparing the statement
     */
    public void name(final String name, final String statement) throws DBIException
    {
        Handle handle = null;
        try
        {
            handle = this.open();
            handle.name(name, statement);
        }
        finally
        {
            if (handle != null) handle.close();
        }
    }

    /**
     * Eagerly load a named query from the filesystem. The name will be <code>name</code>
     * and it will look for a file named <code>[name].sql</code> in the classpath which
     * contains a single sql statement.
     *
     * @param name name of query to load, such as "foo" which will be store din foo.sql
     * @throws IOException
     */
    public void load(final String name) throws DBIException, IOException
    {
        Handle handle = null;
        try
        {
            handle = this.open();
            handle.load(name);
        }
        finally
        {
            if (handle != null) handle.close();
        }
    }

    /**
     * Specify a non-standard <code>TransactionHandler</code> which should be
     * used for all <code>Handle</code> instances created from this dbi.
     * <p />
     * The default handler, if you specify none, will explicitely manage
     * transactions on the underlying JDBC connection.
     *
     * @see org.skife.jdbi.tweak.ConnectionTransactionHandler
     * @see org.skife.jdbi.tweak.CMTConnectionTransactionHandler
     */
    public void setTransactionHandler(TransactionHandler handler)
    {
        this.transactionHandler = handler;
    }

    /**
     * Specify a non-standard statement locator.
     *
     * @param locator used to find externalized sql
     */
    public void setStatementLocator(StatementLocator locator)
    {
        this.repository.setLocator(locator);
    }

    /**
     * Specify a decorator builder to decorate all handles created by this DBI instance
     */
    public void setHandleDecorator(HandleDecorator builder)
    {
        this.handleDecorator = builder;
    }

    /**
     * Obtain a handle directly from a datasource
     */
    public static Handle open(final String connString) throws DBIException
    {
        return new DBI(connString).open();
    }

    /**
     * Obtain a handle directly from a datasource
     */
    public static Handle open(final String wombat, final String name, final String pass) throws DBIException
    {
        return new DBI(wombat, name, pass).open();
    }

    public static Handle open(final DataSource source) throws DBIException
    {
        return new DBI(source).open();
    }

    /**
     * Obtain a handle directly from a datasource
     */
    public static Handle open(final DataSource source, final String name, final String pass) throws DBIException
    {
        return new DBI(source, name, pass).open();
    }

    /**
     * Execute <code>callback</code> with an opened handle, closing the handle, and cleaning
     * up resources when the callback finishes.
     */
    public static void open(final String connString, final HandleCallback callback) throws DBIException
    {
        new DBI(connString).open(callback);
    }

    /**
     * Execute <code>callback</code> with an opened handle, closing the handle, and cleaning
     * up resources when the callback finishes.
     */
    public static void open(final String wombat,
                            final String name,
                            final String pass,
                            final HandleCallback callback) throws DBIException
    {
        new DBI(wombat, name, pass).open(callback);
    }

    /**
     * Execute <code>callback</code> with an opened handle, closing the handle, and cleaning
     * up resources when the callback finishes.
     */
    public static void open(final DataSource source, final HandleCallback callback) throws DBIException
    {
        new DBI(source).open(callback);
    }

    /**
     * Execute <code>callback</code> with an opened handle, closing the handle, and cleaning
     * up resources when the callback finishes.
     */
    public static void open(final DataSource source,
                            final String name,
                            final String pass,
                            final HandleCallback callback) throws DBIException
    {
        new DBI(source, name, pass).open(callback);
    }
}

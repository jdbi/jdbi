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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;
import org.skife.jdbi.v2.tweak.ConnectionFactory;
import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.TransactionHandler;
import org.skife.jdbi.v2.tweak.transactions.LocalTransactionHandler;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Connection;

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

    /**
     * Constructor for use with a DataSource which will provide
     * @param dataSource
     */
    public DBI(DataSource dataSource)
    {
        this(new DataSourceConnectionFactory(dataSource));
        assert(dataSource != null);
    }

    /**
     * Constructor used to allow for obtaining a Connection in a customized manner.
     * <p>
     * The {@link org.skife.jdbi.v2.tweak.ConnectionFactory#openConnection()} method will
     * be invoked to obtain a connection instance whenever a Handle is opened.
     *
     * @param connectionFactory PrvidesJDBC connections to Handle instances
     */
    public DBI(ConnectionFactory connectionFactory)
    {
        assert(connectionFactory != null);
        this.connectionFactory = connectionFactory;
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
        assert(locator != null);
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
        assert(rewriter != null);
        this.statementRewriter = rewriter;
    }

    /**
     * Specify the TransactionHandler instance to use. This allows overriding
     * transaction semantics, or mapping into different transaction
     * management systems.
     * <p>
     * The default version uses local transactions on the database Connection
     * instances obtained.
     *
     * @param handler The TransactionHandler to use for all Handle instances obtained
     *                from this DBI
     */
    public void setTransactionHandler(TransactionHandler handler)
    {
        assert(handler != null);
        this.transactionhandler = handler;
    }

    /**
     * Obtain a Handle to the data source wrapped by this DBI instance
     *
     * @return an open Handle instance
     */
    public Handle open()
    {
        try
        {
            Connection conn = connectionFactory.openConnection();
            PreparedStatementCache cache = new PreparedStatementCache(conn);
            return new BasicHandle(transactionhandler,
                                   statementLocator,
                                   cache,
                                   statementRewriter,
                                   conn);
        }
        catch (SQLException e)
        {
            throw new UnableToObtainConnectionException(e);
        }
    }

    /**
     * Convenience methd used to obtain a handle from a specific data source
     *
     * @param dataSource
     * @return Handle using a Connection obtained from the provided DataSource
     */
    public static Handle open(DataSource dataSource)
    {
        assert(dataSource != null);
        return new DBI(dataSource).open();
    }

    /**
     * Create a Handle wrapping a particular JDBC Connection
     * @param connection
     * @return Handle bound to connection
     */
    public static Handle open(final Connection connection)
    {
        assert(connection != null);
        return new DBI(new ConnectionFactory()
        {
            public Connection openConnection()
            {
                return connection;
            }
        }).open();
    }
}

/* Copyright 2004-2006 Brian McCallister
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

import org.skife.jdbi.v2.exceptions.TransactionFailedException;
import org.skife.jdbi.v2.exceptions.UnableToCloseResourceException;
import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.TransactionHandler;
import org.skife.jdbi.v2.tweak.StatementLocator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class BasicHandle implements Handle
{
    private final TransactionHandler transactions;
    private final Connection connection;
    private StatementRewriter statementRewriter;
    private StatementLocator statementLocator;
    private PreparedStatementCache preparedStatementCache;

//    public BasicHandle(TransactionHandler transactions,
//                       PreparedStatementCache preparedStatementCache,
//                       StatementRewriter statementRewriter,
//                       Connection connection)
//    {
//        this(transactions, new ClasspathStatementLocator(), preparedStatementCache, statementRewriter, connection);
//    }

    public BasicHandle(TransactionHandler transactions,
                       StatementLocator statementLocator,
                       PreparedStatementCache preparedStatementCache,
                       StatementRewriter statementRewriter,
                       Connection connection)
    {
        this.preparedStatementCache = preparedStatementCache;
        this.statementRewriter = statementRewriter;
        this.transactions = transactions;
        this.connection = connection;
        this.statementLocator = statementLocator;
    }

    public Query<Map<String, Object>> createQuery(String sql)
    {
        return new Query<Map<String, Object>>(new Parameters(),
                                              new DefaultMapper(),
                                              statementLocator,
                                              statementRewriter,
                                              connection,
                                              preparedStatementCache,
                                              sql);
    }

    /**
     * Get the JDBC Connection this Handle uses
     *
     * @return the JDBC Connection this Handle uses
     */
    public Connection getConnection()
    {
        return this.connection;
    }

    public void close()
    {
        preparedStatementCache.close();
        try
        {
            connection.close();
        }
        catch (SQLException e)
        {
            throw new UnableToCloseResourceException("Unable to close Connection", e);
        }
    }

    /**
     * Start a transaction
     */
    public Handle begin()
    {
        transactions.begin(this);
        return this;
    }

    /**
     * Commit a transaction
     */
    public Handle commit()
    {
        transactions.commit(this);
        return this;
    }

    /**
     * Rollback a transaction
     */
    public Handle rollback()
    {
        transactions.rollback(this);
        return this;
    }

    public UpdateStatement createStatement(String sql)
    {
        return new UpdateStatement(connection, statementLocator, statementRewriter, preparedStatementCache, sql);
    }

    public int insert(String sql, Object... args)
    {
        return update(sql, args);
    }

    public int update(String sql, Object... args)
    {
        UpdateStatement stmt = createStatement(sql);
        int position = 0;
        for (Object arg : args)
        {
            stmt.bind(position++, arg);
        }
        return stmt.execute();
    }

    public PreparedBatch prepareBatch(String sql)
    {
        return new PreparedBatch(statementLocator, statementRewriter, connection, preparedStatementCache, sql);
    }

    public Batch createBatch()
    {
        return new Batch(this.statementRewriter, this.connection);
    }

    public <ReturnType> ReturnType inTransaction(TransactionCallback<ReturnType> callback) throws TransactionFailedException
    {
        final boolean[] failed = {false};
        TransactionStatus status = new TransactionStatus()
        {
            public void setRollbackOnly()
            {
                failed[0] = true;
            }
        };
        final ReturnType returnValue;
        try
        {
            this.begin();
            returnValue = callback.inTransaction(this, status);
            if (!failed[0])
            {
                this.commit();
            }
        }
        catch (Exception e)
        {
            this.rollback();
            throw new TransactionFailedException("Transaction failed do to exception being thrown " +
                                                 "from within the callback. See cause " +
                                                 "for the original exception.", e);
        }
        if (failed[0])
        {
            this.rollback();
            throw new TransactionFailedException("Transaction failed due to transaction status being set " +
                                                 "to rollback only.");
        }
        else
        {
            return returnValue;
        }
    }

    public List<Map<String, Object>> select(String sql, Object... args)
    {
        Query<Map<String, Object>> query = this.createQuery(sql);
        int position = 0;
        for (Object arg : args)
        {
            query.bind(position++, arg);
        }
        return query.list();
    }

    public void setStatementLocator(StatementLocator locator)
    {
        this.statementLocator = locator;
    }

    public void setStatementRewriter(StatementRewriter rewriter)
    {
        this.statementRewriter = rewriter;
    }

    public Script createScript(String name)
    {
        return new Script(this, statementLocator, name);
    }
}

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

import org.skife.jdbi.v2.exceptions.TransactionFailedException;
import org.skife.jdbi.v2.exceptions.UnableToCloseResourceException;
import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.tweak.StatementRewriter;
import org.skife.jdbi.v2.tweak.TransactionHandler;
import org.skife.jdbi.v2.tweak.SQLLog;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BasicHandle implements Handle
{
    private final TransactionHandler transactions;
    private final Connection connection;
    private StatementRewriter statementRewriter;
    private StatementLocator statementLocator;
    private SQLLog log;
    private StatementBuilder statementBuilder;
    private final Map<String, Object> globalStatementAttributes;

    public BasicHandle(TransactionHandler transactions,
                       StatementLocator statementLocator,
                       StatementBuilder preparedStatementCache,
                       StatementRewriter statementRewriter,
                       Connection connection,
                       Map<String, Object> globalStatementAttributes,
                       SQLLog log)
    {
        this.statementBuilder = preparedStatementCache;
        this.statementRewriter = statementRewriter;
        this.transactions = transactions;
        this.connection = connection;
        this.statementLocator = statementLocator;
        this.log = log;
        this.globalStatementAttributes = new HashMap<String, Object>();
        this.globalStatementAttributes.putAll(globalStatementAttributes);
    }

    public Query<Map<String, Object>> createQuery(String sql)
    {
        return new Query<Map<String, Object>>(new Binding(),
                                              new DefaultMapper(),
                                              statementLocator,
                                              statementRewriter,
                                              connection,
                                              statementBuilder,
                                              sql,
                                              new StatementContext(globalStatementAttributes),
                                              log);
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
        statementBuilder.close(getConnection());
        try {
            connection.close();
            log.logReleaseHandle(this);
        }
        catch (SQLException e) {
            throw new UnableToCloseResourceException("Unable to close Connection", e);
        }
    }

    public void define(String key, Object value)
    {
        this.globalStatementAttributes.put(key, value);
    }

    /**
     * Start a transaction
     */
    public Handle begin()
    {
        transactions.begin(this);
        log.logBeginTransaction(this);
        return this;
    }

    /**
     * Commit a transaction
     */
    public Handle commit()
    {
        final long start = System.currentTimeMillis();
        transactions.commit(this);
        log.logCommitTransaction(System.currentTimeMillis() - start, this);
        return this;
    }

    /**
     * Rollback a transaction
     */
    public Handle rollback()
    {
        final long start = System.currentTimeMillis();
        transactions.rollback(this);
        log.logRollbackTransaction(System.currentTimeMillis() - start,  this);
        return this;
    }

    /**
     * Create a transaction checkpoint (savepoint in JDBC terminology) with the name provided.
     *
     * @param name The name of the checkpoint
     * @return The same handle
     */
    public Handle checkpoint(String name)
    {
        transactions.checkpoint(this, name);
        log.logCheckpointTransaction(this, name);
        return this;
    }

    /**
     * Release the named checkpoint, making rollback to it not possible.
     * @return The same handle
     */
    public Handle release(String checkpointName)
    {
        transactions.release(this, checkpointName);
        log.logReleaseCheckpointTransaction(this, checkpointName);
        return this;
    }

    public void setStatementBuilder(StatementBuilder builder)
    {
        this.statementBuilder = builder;
    }

    public void setSQLLog(SQLLog log)
    {
        this.log = log;
    }

    /**
     * Rollback a transaction to a named checkpoint
     *
     * @param checkpointName the name of the checkpoint, previously declared with {@see Handle#checkpoint}
     */
    public Handle rollback(String checkpointName)
    {
        final long start = System.currentTimeMillis();
        transactions.rollback(this, checkpointName);
        log.logRollbackToCheckpoint(System.currentTimeMillis() - start, this, checkpointName);
        return this;
    }

    public boolean isInTransaction()
    {
        return transactions.isInTransaction(this);
    }

    public Update createStatement(String sql)
    {
        return new Update(connection,
                          statementLocator,
                          statementRewriter,
                          statementBuilder,
                          sql,
                          new StatementContext(globalStatementAttributes),
                          log);
    }

    public <ReturnType> Call<ReturnType> createCall(String sql, CallableStatementMapper<ReturnType> mapper)
    {
        return new Call(connection,
                          statementLocator,
                          statementRewriter,
                          statementBuilder,
                          sql,
                          new StatementContext(globalStatementAttributes),
                          log,
		                  mapper);
    }

    public int insert(String sql, Object... args)
    {
        return update(sql, args);
    }

    public int update(String sql, Object... args)
    {
        Update stmt = createStatement(sql);
        int position = 0;
        for (Object arg : args) {
            stmt.bind(position++, arg);
        }
        return stmt.execute();
    }

    public PreparedBatch prepareBatch(String sql)
    {
        return new PreparedBatch(statementLocator,
                                 statementRewriter,
                                 connection,
                                 statementBuilder,
                                 sql,
                                 globalStatementAttributes,
                                 log);
    }

    public Batch createBatch()
    {
        return new Batch(this.statementRewriter, this.connection, globalStatementAttributes, log);
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
        try {
            this.begin();
            returnValue = callback.inTransaction(this, status);
            if (!failed[0]) {
                this.commit();
            }
        }
        catch (RuntimeException e)
        {
            this.rollback();
            throw e;
        }
        catch (Exception e) {
            this.rollback();
            throw new TransactionFailedException("Transaction failed do to exception being thrown " +
                                                 "from within the callback. See cause " +
                                                 "for the original exception.", e);
        }
        if (failed[0]) {
            this.rollback();
            throw new TransactionFailedException("Transaction failed due to transaction status being set " +
                                                 "to rollback only.");
        }
        else {
            return returnValue;
        }
    }

    public List<Map<String, Object>> select(String sql, Object... args)
    {
        Query<Map<String, Object>> query = this.createQuery(sql);
        int position = 0;
        for (Object arg : args) {
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
        return new Script(this, statementLocator, name, globalStatementAttributes);
    }

    public void execute(String sql, Object... args)
    {
        this.update(sql, args);
    }
}

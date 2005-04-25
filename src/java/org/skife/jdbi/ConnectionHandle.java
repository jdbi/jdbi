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
import org.skife.jdbi.tweak.TransactionHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

class ConnectionHandle implements Handle
{
    private final Connection conn;
    private final StatementCache cache;
    private final TransactionHandler transactionHandler;

    ConnectionHandle(final Connection conn)
    {
        this(conn, new NamedStatementRepository());
    }

    ConnectionHandle(final Connection conn, NamedStatementRepository repository)
    {
        this(conn, repository, new ConnectionTransactionHandler(), new HashMap());
    }

    ConnectionHandle(final Connection conn, 
                     NamedStatementRepository repository, 
                     TransactionHandler transactionHandler,
                     Map globals)
    {
        this.conn = conn;
        this.transactionHandler = transactionHandler;
        this.cache = new StatementCache(conn, repository, new HashMap(globals));
    }

    public int updateInternal(final PreparedStatement stmt) throws DBIException
    {
        try
        {
            return stmt.executeUpdate();
        }
        catch (SQLException e)
        {
            throw new DBIException("error while executing sql: " + e.getMessage(), e);
        }
        finally
        {
            try
            {
                stmt.clearParameters();
            }
            catch (SQLException e)
            {
                throw new DBIException("exception while clearing paramters: " + e.getMessage());
            }
        }
    }

    public void executeBareStatement(final PreparedStatement stmt) throws DBIException
    {
        try
        {
            stmt.execute();
        }
        catch (SQLException e)
        {
            throw new DBIException("error while executing sql: " + e.getMessage(), e);
        }
        finally
        {
            try
            {
                stmt.clearParameters();
            }
            catch (SQLException e)
            {
                throw new DBIException("exception while clearing paramters: " + e.getMessage());
            }
        }
    }

    public Connection getConnection()
    {
        return conn;
    }

    public void begin() throws DBIException
    {
        transactionHandler.begin(this);
    }

    public void close()
    {
        final List errors = new ArrayList();
        try
        {
            clearStatementCacheInternal();
        }
        catch (CacheCloseException e)
        {
            errors.addAll(e.getExceptions());
        }
        try
        {
            conn.close();
        }
        catch (SQLException e)
        {
            errors.add(e);
        }
        if (!errors.isEmpty())
        {
            DBIError top = new DBIError();
            for (Iterator iterator = errors.iterator(); iterator.hasNext();)
            {
                final SQLException error = (SQLException) iterator.next();
                DBIError mine = new DBIError(error);
                top.initCause(mine);
                top = mine;
            }
            throw top;
        }
    }

    public void clearStatementCacheInternal() throws CacheCloseException
    {
        final Collection exceptions = cache.close();
        if (exceptions.isEmpty())
            return;
        else
        {
            CacheCloseException e = new CacheCloseException();
            e.getExceptions().addAll(exceptions);
            throw e;
        }
    }

    public void clearStatementCache()
    {
        try
        {
            clearStatementCacheInternal();
        }
        catch (CacheCloseException e)
        {
            // swallow on purpose
        }
    }

    /* transaction stuff */

    public void commit() throws DBIException
    {
        transactionHandler.commit(this);
    }

    public void rollback() throws DBIException
    {
        transactionHandler.rollback(this);
    }

    public void inTransaction(final TransactionCallback callback) throws DBIException
    {
        begin();
        try
        {
            callback.inTransaction(this);
            commit();
        }
        catch (DBIException e)
        {
            rollback();
            throw e;
        }
        catch (Exception e)
        {
            rollback();
            throw new DBIException("exception thrown from callback: " + e.getMessage(), e);
        }
        catch (Error e)
        {
            rollback();
            throw new DBIError("error thrown from callback: " + e.getMessage(), e);
        }
    }

    public boolean isInTransaction()
    {
        return transactionHandler.isInTransaction(this);
    }

    /* executes */

    public void execute(final String sql) throws DBIException
    {
        this.execute(sql, ParamTool.EMPTY_OBJECT_ARRAY);
    }

    public void execute(final String statement, final Object[] args) throws DBIException
    {
        this.executeBareStatement(cache.find(statement, args));
    }

    public void execute(final String statement, Collection args) throws DBIException
    {
        this.executeBareStatement(cache.find(statement, args));
    }

    public void execute(String statement, Map args) throws DBIException
    {
        this.executeBareStatement(cache.find(statement, args));
    }

    public void execute(String statement, Object bean) throws DBIException
    {
        this.executeBareStatement(cache.find(statement, bean));
    }

    /* updates */

    public int update(String statement) throws DBIException
    {
        return updateInternal(cache.find(statement));
    }

    public int update(String statement, Object[] args) throws DBIException
    {
        return updateInternal(cache.find(statement, args));
    }

    public int update(String statement, Collection args) throws DBIException
    {
        return updateInternal(cache.find(statement, args));
    }

    public int update(String statement, Map args) throws DBIException
    {
        return updateInternal(cache.find(statement, args));
    }

    public int update(String statement, Object bean) throws DBIException
    {
        return updateInternal(cache.find(statement, bean));
    }

    /* Queries */

    public List query(String query) throws DBIException
    {
        return this.queryCollectingResults(cache.find(query, Collections.EMPTY_MAP));
    }

    public void query(String statement, RowCallback callback) throws DBIException
    {
        executeInternal(cache.find(statement), callback);
    }

    public void query(String statement, Object[] args, RowCallback callback) throws DBIException
    {
        executeInternal(cache.find(statement, args), callback);
    }

    public void query(String statement, Map args, RowCallback callback) throws DBIException
    {
        executeInternal(cache.find(statement, args), callback);
    }

    public List query(final String statement, final Map args) throws DBIException
    {
        return this.queryCollectingResults(cache.find(statement, args));
    }

    public List query(String statement, Object param) throws DBIException
    {
        return this.queryCollectingResults(cache.find(statement, param));
    }

    public List query(String statement, Object[] args) throws DBIException
    {
        return this.queryCollectingResults(cache.find(statement, args));
    }

    public List query(String statement, Collection args) throws DBIException
    {
        return this.queryCollectingResults(cache.find(statement, args));
    }

    public Map first(String statement) throws DBIException
    {
        return extractFirst(this.query(statement, Collections.EMPTY_MAP));
    }

    public Map first(String statement, Object bean) throws DBIException
    {
        return extractFirst(this.query(statement, bean));
    }

    public Map first(String statement, Map args) throws DBIException
    {
        return extractFirst(this.query(statement, args));
    }

    public Map first(String statement, Object[] params) throws DBIException
    {
        return extractFirst(this.query(statement, params));
    }

    public Map first(String statement, Collection params) throws DBIException
    {
        return extractFirst(this.query(statement, params));
    }

    private Map extractFirst(List results)
    {
        return (Map) (results.size() > 0 ? results.get(0) : null);
    }

    private List queryCollectingResults(PreparedStatement stmt) throws DBIException
    {
        final List results = new ArrayList();
        this.executeInternal(stmt, new RowCallback()
        {
            public void eachRow(Handle handle, Map row) throws Exception
            {
                results.add(row);
            }
        });
        return results;
    }

    private void executeInternal(final PreparedStatement stmt, final RowCallback callback) throws DBIException
    {
        final ResultSet results;
        try
        {
            results = stmt.executeQuery();
        }
        catch (SQLException e)
        {
            throw new DBIException("error while executing statement: " + e.getMessage(), e);
        }
        try
        {
            final ResultSetMetaData metadata = results.getMetaData();
            final int count = metadata.getColumnCount();
            final String[] columns = new String[count];
            for (int i = 1; i != count + 1; ++i)
            {
                final String column_name = metadata.getColumnName(i);
                final String column_label = metadata.getColumnLabel(i);
                columns[i - 1] = column_label != null ? column_label : column_name;
            }
            while (results.next())
            {
                final Map row = new RowMap();
                for (int i = 0; i != columns.length; i++)
                {
                    final String column = columns[i];
                    final Object value = results.getObject(i + 1);
                    row.put(column, value);
                }
                try
                {
                    callback.eachRow(this, row);
                }
                catch (Exception e)
                {
                    results.close();
                    throw new DBIException("exception while handling results", e);
                }
                catch (Error e)
                {
                    results.close();
                    throw new DBIError("error while handling results", e);
                }
            }
        }
        catch (SQLException e)
        {
            throw new DBIException("exception while reading results: " + e.getMessage(), e);
        }
        finally
        {
            try
            {
                results.close();
            }
            catch (SQLException e)
            {
                throw new DBIException("exception while trying to close resultset: " + e.getMessage(), e);
            }
        }
    }

    public boolean isOpen() throws DBIException
    {
        try
        {
            return !conn.isClosed();
        }
        catch (SQLException e)
        {
            throw new DBIException("exception while querying for open state: " + e.getMessage(), e);
        }
    }

    public void script(final String name) throws DBIException, IOException
    {
        new Script(this, name).run();
    }

    public void name(final String name, final String sql) throws DBIException
    {
        cache.name(name, sql);
    }

    public void load(final String name) throws IOException, DBIException
    {
        final String sql = this.cache.load(name);
        this.cache.name(name, sql);
    }

    public Batch batch()
    {
        return new QueueingBatch(this.getConnection());
    }

    public PreparedBatch prepareBatch(String statement)
    {
        try
        {
            return new QueueingPreparedBatch(cache.find(statement), cache.parametersFor(statement), cache.getGlobals());
        }
        catch (DBIException e)
        {
            throw new DBIError(e.getMessage(), e);
        }
    }

    public Map getGlobalParameters()
    {
        return cache.getGlobals();
    }
}

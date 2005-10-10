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
package org.skife.jdbi.unstable.decorator;

import org.skife.jdbi.Batch;
import org.skife.jdbi.DBIException;
import org.skife.jdbi.Handle;
import org.skife.jdbi.PreparedBatch;
import org.skife.jdbi.RowCallback;
import org.skife.jdbi.TransactionCallback;
import org.skife.jdbi.Query;
import org.skife.jdbi.unstable.Unstable;

import java.io.IOException;
import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * Convenience class implementing Handle which delegates all
 * method calls to the <code>Handle</code> instance passed
 * into its constructor. This is provided to allow
 * decorator instances to only override the bahavior of
 * things they need to.
 */
public class BaseHandleDecorator implements Handle, Unstable
{
    private final Handle handle;

    public BaseHandleDecorator(Handle handle)
    {
        this.handle = handle;
    }

    public Connection getConnection()
    {
        return handle.getConnection();
    }

    public void begin() throws DBIException
    {
        handle.begin();
    }

    public void close()
    {
        handle.close();
    }

    public void clearStatementCache()
    {
        handle.clearStatementCache();
    }

    public void commit() throws DBIException
    {
        handle.commit();
    }

    public void rollback() throws DBIException
    {
        handle.rollback();
    }

    public void inTransaction(final TransactionCallback callback) throws DBIException
    {
        handle.inTransaction(callback);
    }

    public boolean isInTransaction()
    {
        return handle.isInTransaction();
    }

    public void execute(final String sql) throws DBIException
    {
        handle.execute(sql);
    }

    public void execute(final String statement, final Object[] args) throws DBIException
    {
        handle.execute(statement, args);
    }

    public void execute(final String statement, Collection args) throws DBIException
    {
        handle.execute(statement, args);
    }

    public void execute(String statement, Map args) throws DBIException
    {
        handle.execute(statement, args);
    }

    public void execute(String statement, Object bean) throws DBIException
    {
        handle.execute(statement, bean);
    }

    public int update(String statement) throws DBIException
    {
        return handle.update(statement);
    }

    public int update(String statement, Object[] args) throws DBIException
    {
        return handle.update(statement, args);
    }

    public int update(String statement, Collection args) throws DBIException
    {
        return handle.update(statement, args);
    }

    public int update(String statement, Map args) throws DBIException
    {
        return handle.update(statement, args);
    }

    public int update(String statement, Object bean) throws DBIException
    {
        return handle.update(statement, bean);
    }

    public List query(String query) throws DBIException
    {
        return handle.query(query);
    }

    public void query(String statement, RowCallback callback) throws DBIException
    {
        handle.query(statement, callback);
    }

    public void query(String statement, Object[] args, RowCallback callback) throws DBIException
    {
        handle.query(statement, args, callback);
    }

    public void query(String statement, Map args, RowCallback callback) throws DBIException
    {
        handle.query(statement, args, callback);
    }

    public List query(final String statement, final Map args) throws DBIException
    {
        return handle.query(statement, args);
    }

    public List query(String statement, Object param) throws DBIException
    {
        return handle.query(statement, param);
    }

    public List query(String statement, Object[] args) throws DBIException
    {
        return handle.query(statement, args);
    }

    public List query(String statement, Collection args) throws DBIException
    {
        return handle.query(statement, args);
    }

    public Map first(String statement) throws DBIException
    {
        return handle.first(statement);
    }

    public Map first(String statement, Object bean) throws DBIException
    {
        return handle.first(statement, bean);
    }

    public Map first(String statement, Map args) throws DBIException
    {
        return handle.first(statement, args);
    }

    public Map first(String statement, Object[] params) throws DBIException
    {
        return handle.first(statement, params);
    }

    public Map first(String statement, Collection params) throws DBIException
    {
        return handle.first(statement, params);
    }

    public boolean isOpen() throws DBIException
    {
        return handle.isOpen();
    }

    public void script(final String name) throws DBIException, IOException
    {
        handle.script(name);
    }

    public void name(final String name, final String sql) throws DBIException
    {
        handle.name(name, sql);
    }

    public void load(final String name) throws IOException, DBIException
    {
        handle.load(name);
    }

    public Batch batch()
    {
        return handle.batch();
    }

    public PreparedBatch prepareBatch(String statement)
    {
        return handle.prepareBatch(statement);
    }

    public Map getGlobalParameters()
    {
        return handle.getGlobalParameters();
    }

    public Query createQuery(String sql)
    {
        return handle.createQuery(sql);
    }

    public void close(Iterator i)
    {
        handle.close(i);
    }

    public void close(ListIterator i)
    {
        handle.close(i);
    }
}

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

import org.skife.jdbi.tweak.StatementLocator;
import org.skife.jdbi.tweak.TransactionHandler;

import java.io.IOException;
import java.util.Map;

/**
 * Provides an interface based system to access all of the
 * <code>DBI</code> instance methods. Much nicer for proxies.
 */
public interface IDBI
{
    /**
     * Obtain a new Handle instance
     *
     * @return an open Handle
     */
    Handle open() throws DBIException;

    /**
     * Execute the callback with an open handle, closing, and cleaning up resources,
     * after the callback exits or excepts
     *
     * @throws DBIException if exception is thrown from the callback, or
     *                      an exception occurs with the database
     * @throws DBIError     if an Error is thrown from the callback
     */
    void open(HandleCallback callback) throws DBIException;

    /**
     * Obtain an unmodifiable map of all the named statements known to this
     * DBI instance.
     */
    Map getNamedStatements();

    /**
     * Prepared a named sql statement
     *
     * @param name      name to issue query under
     * @param statement sql string to use as query
     * @throws DBIException if there is a problem preparing the statement
     */
    void name(String name, String statement) throws DBIException;

    /**
     * Eagerly load a named query from the filesystem. The name will be <code>name</code>
     * and it will look for a file named <code>[name].sql</code> in the classpath which
     * contains a single sql statement.
     *
     * @param name name of query to load, such as "foo" which will be store din foo.sql
     * @throws IOException
     */
    void load(String name) throws DBIException, IOException;

    /**
     * Specify a non-standard <code>TransactionHandler</code> which should be
     * used for all <code>Handle</code> instances created from this dbi.
     * <p/>
     * The default handler, if you specify none, will explicitely manage
     * transactions on the underlying JDBC connection.
     *
     * @see org.skife.jdbi.tweak.ConnectionTransactionHandler
     * @see org.skife.jdbi.tweak.CMTConnectionTransactionHandler
     */
    void setTransactionHandler(TransactionHandler handler);

    /**
     * Specify a non-standard statement locator.
     *
     * @param locator used to find externalized sql
     */
    void setStatementLocator(StatementLocator locator);

    /**
     * Obtain a map containing globally set named parameter values. All handles obtained
     * from this DBI instance will use these named parameters.
     * <p/>
     * Named parameters added to a handle will not be added to the DBI globals, and DBI globals added
     * after a handle is opened will not be added to the already open handles.
     */
    Map getGlobalParameters();

//    /**
//     * Specify a script locator which will be used when the {@link Handle#script(String)} method
//     * is used for handles created from this DBI instance.
//     * <p/>
//     * The default script locater uses a {@link ChainedScriptLocator} which first attempts a
//     * {@link ClasspathScriptLocator}, then {@link FileSystemScriptLocator}, then finally a
//     * {@link URLScriptLocator}.
//     */
//    void setScriptLocator(ScriptLocator locator);
//
//    /**
//     * Specify a decorator builder to decorate all handles created by this DBI instance
//     */
//    void setHandleDecorator(HandleDecorator builder);
}

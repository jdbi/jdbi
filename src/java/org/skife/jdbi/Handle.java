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

import java.io.IOException;
import java.sql.Connection;
import java.util.Collection;
import java.util.Map;
import java.util.List;

/**
 * Represents a connection to the RDBMS.
 */
public interface Handle
{
    /**
     * Obtain the JDBC connection used by this handle
     */
    Connection getConnection();

    /**
     * Close the connection
     *
     * @throws org.skife.jdbi.DBIError if anything goes really wrong, otherwise just closes
     */
    void close();

    /**
     * Execute an sql statement which does not return any results. This can
     * also be used to execute stored procedures ("call foo()")
     *
     * @param statement insert/update/create/delete/call statement
     */
    void execute(String statement) throws DBIException;

    /**
     * Execute an sql statement which does not return any results. This can
     * also be used to execute stored procedures ("call foo()")
     *
     * @param statement insert/update/create/delete/call statement
     * @param args positional arguments to be bound to <code>statement</code>
     */
    void execute(String statement, Object[] args) throws DBIException;

    /**
     * Execute an sql statement which does not return any results. This can
     * also be used to execute stored procedures ("call foo()")
     *
     * @param statement insert/update/create/delete/call statement
     * @param args positional arguments to be bound to <code>statement</code>
     */
    void execute(String statement, Collection args) throws DBIException;

    /**
     * Execute an sql statement which does not return any results. This can
     * also be used to execute stored procedures ("call foo()")
     *
     * @param statement insert/update/create/delete/call statement
     * @param args named arguments to be bound to <code>statement</code>
     */
    void execute(String statement, Map args) throws DBIException;

    /**
     * Execute a statement with named parameters pulling values from a JavaBean
     *
     * @param statement SQL statement with named parameters
     * @param bean JavaBean with properties to be de-referenced for named parameter substitution
     */
    void execute(String statement, Object bean) throws DBIException;

    /**
     * Execute a statement of the form <code>update foo set bar = foo_id</code>
     *
     * @param statement sql statement or named statement
     * @return number of modified rows
     * @throws DBIException if anything goes wrong
     */
    int update(String statement) throws DBIException;

    /**
     * Execute a statement of the form <code>update foo set bar = foo_id</code>
     *
     * @param statement sql statement or named statement
     * @param args positional args to bindBinaryStream to <code>statement</code>
     * @return number of modified rows
     * @throws DBIException if anything goes wrong
     */
    int update(String statement, Object[] args) throws DBIException;

    /**
     * Execute a statement of the form <code>update foo set bar = foo_id</code>
     *
     * @param statement sql statement or named statement
     * @param args positional args to bindBinaryStream to <code>statement</code>
     * @return number of modified rows
     * @throws DBIException if anything goes wrong
     */
    int update(String statement, Collection args) throws DBIException;

    /**
     * Execute a statement of the form <code>update foo set bar = foo_id</code>
     *
     * @param statement sql statement or named statement
     * @param args named args to bindBinaryStream to <code>statement</code>
     * @return number of modified rows
     * @throws DBIException if anything goes wrong
     */
    int update(String statement, Map args) throws DBIException;

    /**
     * Execute an update with named parameters pulling values from a JavaBean
     *
     * @param statement sql named statement or direct sql
     * @param bean JavaBean whose properties
     * @return number of rows modified
     */
    int update(String statement, Object bean) throws DBIException;

    /**
     * Retrieve a collection of map instances from a query.
     * This is an eagerly loaded collection.
     *
     * @param query select statement
     * @return collection of Map instances
     */
    List query(String query) throws DBIException;

    /**
     * Iterate (once) over a resultset in order calling the callback
     * for each row processed
     *
     * @param select   sql select statement
     * @param callback receive callbacks for each row in result
     */
    void query(String select, RowCallback callback) throws DBIException;

    /**
     * Iterate (once) over a resultset in order calling the callback
     * for each row processed
     *
     * @param statement   sql select statement
     * @param args position arguments to the statement
     * @param callback receive callbacks for each row in result
     */
    void query(String statement, Object[] args, RowCallback callback) throws DBIException;

    /**
     * Iterate (once) over a resultset in order calling the callback
     * for each row processed
     * <p>
     * Named parameters are matched via <code>\s+(:\w+)</code> outside of quotes, 
     * so basically <code>:id</code>, <code>:foo_id</code>, or <code>:id1</code> type 
     * constructions.
     *
     * @param statement   sql select statement
     * @param args named arguments to the statement
     * @param callback receive callbacks for each row in result
     */
    void query(String statement, Map args, RowCallback callback) throws DBIException;

        /**
     * Execute query using name parameters of the form:
     * <code>select id, name from something where id = :something</code> and the
     * key to the params map is "something"
     * <p>
     * Named parameters are matched via <code>\s+(:\w+)</code> outside of quotes,
     * so basically <code>:id</code>, <code>:foo_id</code>, or <code>:id1</code> type
     * constructions.
     *
     * @param statement sql statement
     * @param params    map of named parameters
     * @return collection of Map instances with results
     */
    List query(String statement, Map params) throws DBIException;

    /**
     * Execute statement with JavaBean mapped named parameter
     *
     * @param statement sql or named statement with named paramaters
     * @param bean JavaBean whose properties will be used to populate named parameters
     * @return results
     * @throws DBIException
     */
    List query(String statement, Object bean) throws DBIException;

    /**
     * Execute statement with positional arguments
     *
     * @param statement sql or named statement
     * @param params    positional parameters
     * @return results
     * @throws DBIException
     */
    List query(String statement, Object[] params) throws DBIException;

    /**
     * Execute statement with positional arguments
     *
     * @param statement sql or named statement
     * @param args positional parameters, bound in iteration order
     * @return results
     * @throws DBIException
     */
    List query(String statement, Collection args) throws DBIException;

    /**
     * Returns the first row matched by the query
     *
     * @param statement select statement or named query
     * @return first row
     */
    Map first(String statement) throws DBIException;

    /**
     * Returns the first row matched by the query
     *
     * @param statement select statement or named query
     * @param bean JavaBean whose properties will be used to populate named parameters
     * @return first row
     */
    Map first(String statement, Object bean) throws DBIException;

    /**
     * Returns the first row matched by the query
     *
     * @param statement select statement or named query
     * @param args map of named parameters
     * @return first row
     */
    Map first(String statement, Map args) throws DBIException;

    /**
     * Returns the first row matched by the query
     *
     * @param statement select statement or named query
     * @param params positional parameters
     * @return first row
     */
    Map first(String statement, Object[] params) throws DBIException;


    /**
     * Returns the first row matched by the query
     *
     * @param statement select statement or named query
     * @param params positional parameters
     * @return first row
     */
    Map first(String statement, Collection params) throws DBIException;

    /**
     * start a transaction
     */
    void begin() throws DBIException;

    /**
     * Commit transaction in progress
     */
    void commit() throws DBIException;

    /**
     * Prepared a named sql statement
     *
     * @param name name to issue query under
     * @param sql  sql string to use as query
     * @throws DBIException if there is a problem preparing the statement
     */
    void name(String name, String sql) throws DBIException;

    /**
     * Eagerly load a named query from the filesystem. The name will be <code>name</code>
     * and it will look for a file named <code>[name].sql</code> in the classpath which
     * contains a single sql statement.
     *
     * @param name name of query to load, such as "foo" which will be store din foo.sql
     * @throws IOException
     */
    void load(String name) throws IOException, DBIException;

    /**
     * Execute <code>transactionCallback</code> in a transaction, cleaning up
     * as necesary around it
     */
    void inTransaction(TransactionCallback transactionCallback) throws DBIException;

    /**
     * Has a transaction been started?
     */
    boolean isInTransaction();

    /**
     * Checks to make sure the connection is live
     *
     * @throws DBIException
     */
    boolean isOpen() throws DBIException;

    /**
     * Find and execute the sql script <code>name</code>. First it will be search for
     * <code>name.sql</code>, or, if that is not found, <code>name</code> will be loaded
     * directly.
     * <p>
     * Scripts should seperate statements with a semicolon, for example:
     * <pre><code>
     * create table wombats (
     *  wombat_id integer primary key,
     *  name varchar(50)
     * );
     *
     * insert into wombats (wombat_id, name) values (1, 'Muggie');
     *
     * call wiggles(1, 2, 3);
     * </code></pre>
     *
     * @param name
     * @throws DBIException
     * @throws IOException
     */
    void script(String name) throws DBIException, IOException;

    /**
     * Rollback a transaction in progress
     *
     * @throws DBIException if the rollback fails
     */
    void rollback() throws DBIException;

    /**
     * Clear this handle's cache of prepared statements. Will be called
     * automatically prior to closing the handle, or can be cleared
     * manually at any point.
     */
    void clearStatementCache();

    /**
     * Create a new Batch instance which can be used to queue up and execute statements in
     * a single batch.
     */
    Batch batch();

    /**
     * Create a new PreparedBatch instance from arbitrary SQL or a named statement
     */
    PreparedBatch prepareBatch(String statement);

    /**
     * Obtain a map containing globally set named parameter values. All statements with named parameters will
     * be able to make use of the global named params. Parameters passed in will overlay global params.
     * <p>
     * Handles create a local copy of global parameters specified on the DBI instance used to create the handle.
     * Global parameters added to the Handle will not be added to the DBI instance's globals, however.
     */
    Map getGlobalParameters();
}

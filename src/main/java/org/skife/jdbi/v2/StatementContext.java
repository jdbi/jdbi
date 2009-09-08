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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

/**
 * The statement context provides a means for passing client specific information through the
 * evaluation of a statement. The context is not used by jDBI internally, but will be passed
 * to all statement customizers. This makes it possible to parameterize the processing of
 * the tweakable parts of the statement processing cycle.
 */
public class StatementContext
{
    /** The statement loader can store the group in which it found a statemnt using this key. */
    public static final String STATEMENT_GROUP = "_jdbi_group";

    /** The statement loader can store the name of a statemnt using this key. */
    public static final String STATEMENT_NAME = "_jdbi_name";

    private final Map<String, Object> attributes = new HashMap<String, Object>();
    private String rawSql;
    private String rewrittenSql;
    private String locatedSql;
    private PreparedStatement statement;
    private Connection connection;
    private Binding binding;

    StatementContext(Map<String, Object> globalAttributes) {
        attributes.putAll(globalAttributes);
    }

    /**
     * Specify an attribute on the statement context
     *
     * @param key   name of the attribute
     * @param value value for the attribute
     *
     * @return previous value of this attribute
     */
    public Object setAttribute(String key, Object value)
    {
        return attributes.put(key, value);
    }

    /**
     * Obtain the value of an attribute
     *
     * @param key The name of the attribute
     * @return the value of the attribute
     */
    public Object getAttribute(String key)
    {
        return this.attributes.get(key);
    }

    /**
     * Obtain all the attributes associated with this context as a map. Changes to the map
     * or to the attributes on the context will be reflected across both
     * @return a map f attributes
     */
    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    void setRawSql(String rawSql) {
        this.rawSql = rawSql;
    }

    /**
     * Obtain the initial sql for the statement used to create the statement
     * @return the initial sql
     */
    public String getRawSql() {
        return rawSql;
    }

    void setLocatedSql(String locatedSql) {
        this.locatedSql = locatedSql;
    }

    void setRewrittenSql(String rewrittenSql) {
        this.rewrittenSql = rewrittenSql;
    }

    /**
     * Obtain the located and rewritten sql
     *
     * Not available until until statement execution time
     * @return the sql as it will be executed against the database
     */
    public String getRewrittenSql() {
        return rewrittenSql;
    }

    /**
     * Obtain the located sql
     *
     * Not available until until statement execution time
     * @return the sql which will be passed to the statement rewriter
     */
    public String getLocatedSql() {
        return locatedSql;
    }

    void setStatement(PreparedStatement stmt) {
        statement = stmt;
    }

    /**
     * Obtain the actual prepared statement being used.
     *
     * Not available until execution time
     * @return Obtain the actual prepared statement being used.
     */
    public PreparedStatement getStatement() {
        return statement;
    }

    void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * Obtain the JDBC connection being used for this statement
     * @return the JDBC connection
     */
    public Connection getConnection() {
        return connection;
    }

    public void setBinding(Binding b) {
        this.binding = b;
    }

    public Binding getBinding() {
        return binding;
    }
}

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

import org.skife.jdbi.v2.tweak.ResultColumnMapper;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;

/**
 * The statement context provides a means for passing client specific information through the
 * evaluation of a statement. The context is not used by jDBI internally, but will be passed
 * to all statement customizers. This makes it possible to parameterize the processing of
 * the tweakable parts of the statement processing cycle.
 */
public interface StatementContext
{
    /**
     * Specify an attribute on the statement context
     *
     * @param key   name of the attribute
     * @param value value for the attribute
     *
     * @return previous value of this attribute
     */
    Object setAttribute(String key, Object value);

    /**
     * Obtain the value of an attribute
     *
     * @param key The name of the attribute
     *
     * @return the value of the attribute
     */
    Object getAttribute(String key);

    /**
     * Obtain all the attributes associated with this context as a map. Changes to the map
     * or to the attributes on the context will be reflected across both
     *
     * @return a map f attributes
     */
    Map<String, Object> getAttributes();

    /**
     * Obtain a column mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a ResultColumnMapper for the given type, or null if no column mapper is registered for the given type.
     */
    ResultColumnMapper columnMapperFor(Class type);

    /**
     * Obtain the initial sql for the statement used to create the statement
     *
     * @return the initial sql
     */
    String getRawSql();

    /**
     * Obtain the located and rewritten sql
     * <p/>
     * Not available until until statement execution time
     *
     * @return the sql as it will be executed against the database
     */
    String getRewrittenSql();

    /**
     * Obtain the located sql
     * <p/>
     * Not available until until statement execution time
     *
     * @return the sql which will be passed to the statement rewriter
     */
    String getLocatedSql();

    /**
     * Obtain the actual prepared statement being used.
     * <p/>
     * Not available until execution time
     *
     * @return Obtain the actual prepared statement being used.
     */
    PreparedStatement getStatement();

    /**
     * Obtain the JDBC connection being used for this statement
     *
     * @return the JDBC connection
     */
    Connection getConnection();

    Binding getBinding();

    Class<?> getSqlObjectType();

    Method getSqlObjectMethod();

    /**
     * Is the statement being generated expected to return the generated keys?
     */
    boolean isReturningGeneratedKeys();

    String[] getGeneratedKeysColumnNames();

    void addCleanable(Cleanable cleanable);

    /**
     * Return if the statement should be concurrent updatable.
     *
     * If this returns true, the concurrency level of the created ResultSet will be
     * {@link java.sql.ResultSet#CONCUR_UPDATABLE}, otherwise the result set is not updatable,
     * and will have concurrency level {@link java.sql.ResultSet#CONCUR_READ_ONLY}.
     *
     * @return if the statement generated should be concurrent updatable.
     */
    boolean isConcurrentUpdatable();

    /**
     * Obtain the Foreman that can be used to create dynamically determined Argument
     * instances given the registered ArgumentFactory instances.
     */
    Foreman getForeman();

}

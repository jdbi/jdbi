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
package org.jdbi.v3.core;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;

/**
 * The statement context provides a means for passing client specific information through the
 * evaluation of a statement. The context is not used by jDBI internally, but will be passed
 * to all statement customizers. This makes it possible to parameterize the processing of
 * the tweakable parts of the statement processing cycle.
 */
public interface StatementContext {
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
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    Optional<ColumnMapper<?>> findColumnMapperFor(Type type);

    /**
     * Obtain a row mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    Optional<RowMapper<?>> findRowMapperFor(Type type);

    /**
     * Obtain an argument for given value in this context
     * @param type the type of the argument.
     * @param value the argument value.
     * @return an Argument for the given value.
     */
    Optional<Argument> findArgumentFor(Type type, Object value);

    /**
     * Obtain a collector for the given type in this context
     * @param type the result type of the collector
     * @return a Collector for the given result type, or null if no collector factory is registered for this type.
     */
    Optional<Collector<?, ?, ?>> findCollectorFor(Type type);

    /**
     * @param containerType the container type.
     * @return the element type for the given container type, if available.
     */
    Optional<Type> elementTypeFor(Type containerType);

    /**
     * Obtain the initial sql for the statement used to create the statement
     *
     * @return the initial sql
     */
    String getRawSql();

    /**
     * Obtain the located and rewritten sql
     * <p>
     * Not available until until statement execution time
     * </p>
     *
     * @return the sql as it will be executed against the database
     */
    String getRewrittenSql();

    /**
     * Obtain the actual prepared statement being used.
     * <p>
     * Not available until execution time
     * </p>
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

    ExtensionMethod getExtensionMethod();

    /**
     * @return whether the statement being generated is expected to return generated keys.
     */
    boolean isReturningGeneratedKeys();

    String[] getGeneratedKeysColumnNames();

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
}

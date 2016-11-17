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
package org.jdbi.v3.core.array;

import static org.jdbi.v3.core.internal.JdbiOptionals.findFirstPresent;
import static org.jdbi.v3.core.internal.JdbiStreams.toStream;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.core.JdbiConfig;
import org.jdbi.v3.core.StatementContext;

/**
 * Configuration class for SQL array binding and mapping.
 */
public class SqlArrayTypes implements JdbiConfig<SqlArrayTypes> {

    private final Optional<SqlArrayTypes> parent;
    private final List<SqlArrayTypeFactory> factories = new CopyOnWriteArrayList<>();

    private SqlArrayArgumentStrategy argumentStrategy = SqlArrayArgumentStrategy.SQL_ARRAY;

    public SqlArrayTypes() {
        parent = Optional.empty();
        argumentStrategy = SqlArrayArgumentStrategy.SQL_ARRAY;
    }

    private SqlArrayTypes(SqlArrayTypes parent) {
        this.parent = Optional.of(parent);
        this.argumentStrategy = parent.argumentStrategy;
    }

    /**
     * Returns the strategy used to bind array-type arguments to SQL statements.
     */
    public SqlArrayArgumentStrategy getArgumentStrategy() {
        return argumentStrategy;
    }

    /**
     * Sets the strategy used when binding array-type arguments to SQL statements.
     *
     * @param argumentStrategy the argument strategy to set
     * @return this
     */
    public SqlArrayTypes setArgumentStrategy(SqlArrayArgumentStrategy argumentStrategy) {
        this.argumentStrategy = argumentStrategy;
        return this;
    }

    /**
     * Register an array element type that is supported by the JDBC vendor.
     *
     * @param elementType the array element type
     * @param sqlTypeName the vendor-specific SQL type name for the array type.  This value will be passed to
     *                    {@link java.sql.Connection#createArrayOf(String, Object[])} to create SQL arrays.
     * @return this
     */
    public SqlArrayTypes register(Class<?> elementType, String sqlTypeName) {
        return register(VendorSupportedArrayType.factory(elementType, sqlTypeName));
    }

    /**
     * Register a {@link SqlArrayType} which will have its parameterized type inspected to determine which element type
     * it supports. {@link SqlArrayType SQL array types} are used to convert array-like arguments into SQL arrays.
     * <p>
     * The parameter must be concretely parameterized; we use the type argument {@code T} to determine if it applies to
     * a given element type.
     *
     * @param arrayType the {@link SqlArrayType}
     * @return this
     * @throws UnsupportedOperationException if the argument is not a concretely parameterized type
     */
    public SqlArrayTypes register(SqlArrayType<?> arrayType) {
        return register(new InferredSqlArrayTypeFactory(arrayType));
    }

    /**
     * Register a {@link SqlArrayTypeFactory}. A factory is provided element types and, if it supports it, provides an
     * {@link SqlArrayType} for it.
     *
     * @param factory the factory
     * @return this
     */
    public SqlArrayTypes register(SqlArrayTypeFactory factory) {
        factories.add(0, factory);
        return this;
    }

    /**
     * Obtain an {@link SqlArrayType} for the given array element type in the given context
     *
     * @param elementType the array element type.
     * @param ctx         the statement context
     * @return an {@link SqlArrayType} for the given element type.
     */
    public Optional<SqlArrayType<?>> findFor(Type elementType, StatementContext ctx) {
        return findFirstPresent(
                () -> factories.stream()
                        .flatMap(factory -> toStream(factory.build(elementType, ctx)))
                        .findFirst(),
                () -> parent.flatMap(p -> p.findFor(elementType, ctx)));
    }

    @Override
    public SqlArrayTypes createChild() {
        return new SqlArrayTypes(this);
    }
}

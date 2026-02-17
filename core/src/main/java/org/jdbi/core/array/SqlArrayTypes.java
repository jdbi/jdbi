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
package org.jdbi.core.array;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.enums.internal.EnumSqlArrayTypeFactory;
import org.jdbi.core.interceptor.JdbiInterceptionChainHolder;
import org.jdbi.meta.Alpha;

/**
 * Configuration class for SQL array binding and mapping.
 */
public class SqlArrayTypes implements JdbiConfig<SqlArrayTypes> {

    private final JdbiInterceptionChainHolder<SqlArrayType<?>, SqlArrayTypeFactory> inferenceInterceptors;

    private final List<SqlArrayTypeFactory> factories;
    private SqlArrayArgumentStrategy argumentStrategy;

    private ConfigRegistry registry;

    public SqlArrayTypes() {
        inferenceInterceptors = new JdbiInterceptionChainHolder<>(InferredSqlArrayTypeFactory::new);
        factories = new CopyOnWriteArrayList<>();
        argumentStrategy = SqlArrayArgumentStrategy.SQL_ARRAY;

        register(boolean.class, "boolean");
        register(Boolean.class, "boolean");
        register(short.class, "smallint");
        register(Short.class, "smallint");
        register(int.class, "integer");
        register(Integer.class, "integer");
        register(long.class, "bigint");
        register(Long.class, "bigint");
        register(float.class, "float4");
        register(Float.class, "float4");
        register(double.class, "float8");
        register(Double.class, "float8");
        register(String.class, "varchar");
        register(UUID.class, "uuid");
        register(new EnumSqlArrayTypeFactory());
    }

    private SqlArrayTypes(SqlArrayTypes that) {
        factories = new CopyOnWriteArrayList<>(that.factories);
        argumentStrategy = that.argumentStrategy;
        inferenceInterceptors = new JdbiInterceptionChainHolder<>(that.inferenceInterceptors);
    }

    /**
     * Returns the strategy used to bind array-type arguments to SQL statements.
     *
     * @return the strategy used to bind array-type arguments to SQL statements
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
        return register(SqlArrayTypeFactory.of(elementType, sqlTypeName, Function.identity()));
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
        SqlArrayTypeFactory factory = inferenceInterceptors.process(arrayType);

        return register(factory);
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
     * @return an {@link SqlArrayType} for the given element type.
     */
    public Optional<SqlArrayType<?>> findFor(Type elementType) {
        return factories.stream()
                .flatMap(factory -> factory.build(elementType, registry).stream())
                .findFirst();
    }

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

    /**
     * Returns the {@link JdbiInterceptionChainHolder} for the SqlArrayType inference. This chain allows registration of custom interceptors to change the standard
     * type inference for the {@link SqlArrayTypes#register(SqlArrayType)} method.
     */
    @Alpha
    public JdbiInterceptionChainHolder<SqlArrayType<?>, SqlArrayTypeFactory> getInferenceInterceptors() {
        return inferenceInterceptors;
    }

    @Override
    public SqlArrayTypes createCopy() {
        return new SqlArrayTypes(this);
    }
}

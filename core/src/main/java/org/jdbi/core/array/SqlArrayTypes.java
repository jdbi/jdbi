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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.enums.internal.EnumSqlArrayTypeFactory;
import org.jdbi.core.interceptor.JdbiInterceptionChainHolder;
import org.jdbi.core.interceptor.JdbiInterceptor;
import org.jdbi.core.internal.RegistrationLists;
import org.jdbi.meta.Alpha;

/**
 * Configuration class for SQL array binding and mapping. Holds only registration data and the argument
 * strategy; resolving a factory into a {@link SqlArrayType} for a given element type is done per
 * configuration registry by {@link ArrayTypeResolver}.
 * <p>
 * This configuration is immutable: {@link #register}, {@link #withInferenceInterceptor} and
 * {@link #argumentStrategy(SqlArrayArgumentStrategy)} return a new instance, leaving the receiver unchanged.
 */
public final class SqlArrayTypes implements JdbiConfig<SqlArrayTypes> {

    private final JdbiInterceptionChainHolder<SqlArrayType<?>, SqlArrayTypeFactory> inferenceInterceptors;

    private final List<SqlArrayTypeFactory> factories;
    private final SqlArrayArgumentStrategy argumentStrategy;

    public SqlArrayTypes() {
        this(buildDefaultFactories(), new JdbiInterceptionChainHolder<>(InferredSqlArrayTypeFactory::new),
                SqlArrayArgumentStrategy.SQL_ARRAY);
    }

    private SqlArrayTypes(final List<SqlArrayTypeFactory> factories,
            final JdbiInterceptionChainHolder<SqlArrayType<?>, SqlArrayTypeFactory> inferenceInterceptors,
            final SqlArrayArgumentStrategy argumentStrategy) {
        this.factories = factories;
        this.inferenceInterceptors = inferenceInterceptors;
        this.argumentStrategy = argumentStrategy;
    }

    private static List<SqlArrayTypeFactory> buildDefaultFactories() {
        // Registration prepends, so the effective consultation order is the reverse of registration order.
        final List<SqlArrayTypeFactory> factories = new ArrayList<>();
        prepend(factories, boolean.class, "boolean");
        prepend(factories, Boolean.class, "boolean");
        prepend(factories, short.class, "smallint");
        prepend(factories, Short.class, "smallint");
        prepend(factories, int.class, "integer");
        prepend(factories, Integer.class, "integer");
        prepend(factories, long.class, "bigint");
        prepend(factories, Long.class, "bigint");
        prepend(factories, float.class, "float4");
        prepend(factories, Float.class, "float4");
        prepend(factories, double.class, "float8");
        prepend(factories, Double.class, "float8");
        prepend(factories, String.class, "varchar");
        prepend(factories, UUID.class, "uuid");
        factories.add(0, new EnumSqlArrayTypeFactory());
        return List.copyOf(factories);
    }

    private static void prepend(final List<SqlArrayTypeFactory> factories, final Class<?> elementType, final String sqlTypeName) {
        factories.add(0, SqlArrayTypeFactory.of(elementType, sqlTypeName, Function.identity()));
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
     * Returns a copy of this configuration with the given strategy used when binding array-type arguments to
     * SQL statements.
     *
     * @param argumentStrategy the argument strategy to set
     * @return the derived configuration
     */
    @CheckReturnValue
    public SqlArrayTypes argumentStrategy(SqlArrayArgumentStrategy argumentStrategy) {
        return new SqlArrayTypes(factories, inferenceInterceptors, argumentStrategy);
    }

    /**
     * Returns a copy of this configuration with the given interceptor added to the front of the SqlArrayType
     * inference chain, letting it change the standard type inference for {@link #register(SqlArrayType)}.
     *
     * @param interceptor the inference interceptor to add
     * @return the derived configuration
     */
    @CheckReturnValue
    @Alpha
    public SqlArrayTypes withInferenceInterceptor(final JdbiInterceptor<SqlArrayType<?>, SqlArrayTypeFactory> interceptor) {
        final JdbiInterceptionChainHolder<SqlArrayType<?>, SqlArrayTypeFactory> newInterceptors = new JdbiInterceptionChainHolder<>(inferenceInterceptors);
        newInterceptors.addFirst(interceptor);
        return new SqlArrayTypes(factories, newInterceptors, argumentStrategy);
    }

    /**
     * Register an array element type that is supported by the JDBC vendor.
     *
     * @param elementType the array element type
     * @param sqlTypeName the vendor-specific SQL type name for the array type.  This value will be passed to
     *                    {@link java.sql.Connection#createArrayOf(String, Object[])} to create SQL arrays.
     * @return a copy of this configuration with the array type registered
     */
    @CheckReturnValue
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
     * @return a copy of this configuration with the array type registered
     * @throws UnsupportedOperationException if the argument is not a concretely parameterized type
     */
    @CheckReturnValue
    public SqlArrayTypes register(SqlArrayType<?> arrayType) {
        SqlArrayTypeFactory factory = inferenceInterceptors.process(arrayType);

        return register(factory);
    }

    /**
     * Register a {@link SqlArrayTypeFactory}. A factory is provided element types and, if it supports it, provides an
     * {@link SqlArrayType} for it.
     *
     * @param factory the factory
     * @return a copy of this configuration with the factory registered
     */
    @CheckReturnValue
    public SqlArrayTypes register(SqlArrayTypeFactory factory) {
        return new SqlArrayTypes(RegistrationLists.prepend(factories, factory), inferenceInterceptors, argumentStrategy);
    }

    /**
     * Returns the registered factories, most-recently-registered first. Consumed by {@link ArrayTypeResolver}.
     *
     * @return the registered array-type factories
     */
    List<SqlArrayTypeFactory> getFactories() {
        return factories;
    }

}

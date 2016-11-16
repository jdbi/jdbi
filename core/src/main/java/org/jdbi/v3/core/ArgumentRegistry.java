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

import static org.jdbi.v3.core.internal.JdbiOptionals.findFirstPresent;
import static org.jdbi.v3.core.internal.JdbiStreams.toStream;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.BuiltInArgumentFactory;
import org.jdbi.v3.core.argument.SqlArrayType;
import org.jdbi.v3.core.argument.SqlArrayTypeFactory;

public class ArgumentRegistry implements JdbiConfig<ArgumentRegistry> {

    private final Optional<ArgumentRegistry> parent;
    private final List<ArgumentFactory> argumentFactories = new CopyOnWriteArrayList<>();
    private final List<SqlArrayTypeFactory> arrayTypeFactories = new CopyOnWriteArrayList<>();

    public ArgumentRegistry() {
        parent = Optional.empty();
        register(BuiltInArgumentFactory.INSTANCE);
        register(new SqlArrayArgumentFactory());
    }

    private ArgumentRegistry(ArgumentRegistry that) {
        parent = Optional.of(that);
    }

    /**
     * Obtain an argument for given value in the given context
     * @param expectedType the type of the argument.
     * @param value the argument value.
     * @param ctx the statement context.
     * @return an Argument for the given value.
     */
    public Optional<Argument> findArgumentFor(Type expectedType, Object value, StatementContext ctx) {
        return findFirstPresent(
                () -> argumentFactories.stream()
                        .flatMap(factory -> toStream(factory.build(expectedType, value, ctx)))
                        .findFirst(),
                () -> parent.flatMap(p -> p.findArgumentFor(expectedType, value, ctx)));
    }

    public ArgumentRegistry register(ArgumentFactory factory) {
        argumentFactories.add(0, factory);
        return this;
    }

    /**
     * Obtain an {@link SqlArrayType} for the given array element type in the given context
     * @param elementType the array element type.
     * @param ctx the statement context
     * @return an {@link SqlArrayType} for the given element type.
     */
    public Optional<SqlArrayType<?>> findArrayTypeFor(Type elementType, StatementContext ctx) {
        return findFirstPresent(
                () -> arrayTypeFactories.stream()
                        .flatMap(factory -> toStream(factory.build(elementType, ctx)))
                        .findFirst(),
                () -> parent.flatMap(p -> p.findArrayTypeFor(elementType, ctx)));
    }

    /**
     * Register an array element type that is supported by the JDBC vendor.
     *
     * @param elementType the array element type
     * @param sqlTypeName the vendor-specific SQL type name for the array type.  This value will be passed to
     *                    {@link java.sql.Connection#createArrayOf(String, Object[])} to create SQL arrays.
     * @return this
     */
    public ArgumentRegistry registerArrayType(Class<?> elementType, String sqlTypeName) {
        return registerArrayType(VendorSupportedArrayType.factory(elementType, sqlTypeName));
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
    public ArgumentRegistry registerArrayType(SqlArrayType<?> arrayType) {
        return registerArrayType(new InferredSqlArrayTypeFactory(arrayType));
    }

    /**
     * Register a {@link SqlArrayTypeFactory}. A factory is provided element types and, if it supports it, provides an
     * {@link SqlArrayType} for it.
     *
     * @param factory the factory
     * @return this
     */
    public ArgumentRegistry registerArrayType(SqlArrayTypeFactory factory) {
        arrayTypeFactories.add(0, factory);
        return this;
    }

    @Override
    public ArgumentRegistry createChild() {
        return new ArgumentRegistry(this);
    }
}

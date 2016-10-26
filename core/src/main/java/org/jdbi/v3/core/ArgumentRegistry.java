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

import static org.jdbi.v3.core.internal.JdbiStreams.toStream;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.SqlArrayType;
import org.jdbi.v3.core.argument.SqlArrayTypeFactory;
import org.jdbi.v3.core.argument.BuiltInArgumentFactory;

class ArgumentRegistry
{

    static ArgumentRegistry copyOf(ArgumentRegistry registry) {
        return new ArgumentRegistry(registry);
    }

    private final List<ArgumentFactory> argumentFactories = new CopyOnWriteArrayList<>();
    private final List<SqlArrayTypeFactory> arrayTypeFactories = new CopyOnWriteArrayList<>();

    ArgumentRegistry()
    {
        register(BuiltInArgumentFactory.INSTANCE);
        register(new SqlArrayArgumentFactory());
    }

    ArgumentRegistry(ArgumentRegistry that)
    {
        this.argumentFactories.addAll(that.argumentFactories);
        this.arrayTypeFactories.addAll(that.arrayTypeFactories);
    }

    Optional<Argument> findArgumentFor(Type expectedType, Object it, StatementContext ctx)
    {
        return argumentFactories.stream()
                .flatMap(factory -> toStream(factory.build(expectedType, it, ctx)))
                .findFirst();
    }

    void register(ArgumentFactory factory)
    {
        argumentFactories.add(0, factory);
    }

    Optional<SqlArrayType<?>> findArrayTypeFor(Type elementType, StatementContext ctx) {
        return arrayTypeFactories.stream()
                .flatMap(factory -> toStream(factory.build(elementType, ctx)))
                .findFirst();
    }

    void registerArrayType(Class<?> elementType, String sqlTypeName) {
        registerArrayType(VendorSupportedArrayType.factory(elementType, sqlTypeName));
    }

    void registerArrayType(SqlArrayType<?> arrayType) {
        registerArrayType(new InferredSqlArrayTypeFactory(arrayType));
    }

    void registerArrayType(SqlArrayTypeFactory factory) {
        arrayTypeFactories.add(0, factory);
    }
}

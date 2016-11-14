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
        registerArgumentFactory(BuiltInArgumentFactory.INSTANCE);
        registerArgumentFactory(new SqlArrayArgumentFactory());
    }

    private ArgumentRegistry(ArgumentRegistry that) {
        parent = Optional.of(that);
    }

    public Optional<Argument> findArgumentFor(Type expectedType, Object it, StatementContext ctx) {
        return findFirstPresent(
                () -> argumentFactories.stream()
                        .flatMap(factory -> toStream(factory.build(expectedType, it, ctx)))
                        .findFirst(),
                () -> parent.flatMap(p -> p.findArgumentFor(expectedType, it, ctx)));
    }

    public void registerArgumentFactory(ArgumentFactory factory) {
        argumentFactories.add(0, factory);
    }

    public Optional<SqlArrayType<?>> findArrayTypeFor(Type elementType, StatementContext ctx) {
        return findFirstPresent(
                () -> arrayTypeFactories.stream()
                        .flatMap(factory -> toStream(factory.build(elementType, ctx)))
                        .findFirst(),
                () -> parent.flatMap(p -> p.findArrayTypeFor(elementType, ctx)));
    }

    public void registerArrayType(Class<?> elementType, String sqlTypeName) {
        registerArrayType(VendorSupportedArrayType.factory(elementType, sqlTypeName));
    }

    public void registerArrayType(SqlArrayType<?> arrayType) {
        registerArrayType(new InferredSqlArrayTypeFactory(arrayType));
    }

    public void registerArrayType(SqlArrayTypeFactory factory) {
        arrayTypeFactories.add(0, factory);
    }

    @Override
    public ArgumentRegistry createChild() {
        return new ArgumentRegistry(this);
    }
}

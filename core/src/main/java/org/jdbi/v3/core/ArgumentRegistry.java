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
import org.jdbi.v3.core.argument.ArrayElementMapper;
import org.jdbi.v3.core.argument.ArrayElementMapperFactory;
import org.jdbi.v3.core.argument.BuiltInArgumentFactory;

class ArgumentRegistry
{

    static ArgumentRegistry copyOf(ArgumentRegistry registry) {
        return new ArgumentRegistry(registry);
    }

    private final List<ArgumentFactory> argumentFactories = new CopyOnWriteArrayList<>();
    private final List<ArrayElementMapperFactory> arrayElementMapperFactories = new CopyOnWriteArrayList<>();

    ArgumentRegistry()
    {
        register(BuiltInArgumentFactory.INSTANCE);
        register(new SqlArrayArgumentFactory());
    }

    ArgumentRegistry(ArgumentRegistry that)
    {
        this.argumentFactories.addAll(that.argumentFactories);
        this.arrayElementMapperFactories.addAll(that.arrayElementMapperFactories);
    }

    Optional<Argument> findArgumentFor(Type expectedType, Object it, StatementContext ctx)
    {
        return argumentFactories.stream()
                .flatMap(factory -> toStream(factory.build(expectedType, it, ctx)))
                .findFirst();
    }

    Optional<ArrayElementMapper<?>> findArrayElementMapperFor(Type expectedType, StatementContext ctx) {
        return arrayElementMapperFactories.stream()
                .flatMap(factory -> toStream(factory.build(expectedType, ctx)))
                .findFirst();
    }

    void register(ArgumentFactory factory)
    {
        argumentFactories.add(0, factory);
    }

    void register(ArrayElementMapperFactory factory) {
        arrayElementMapperFactories.add(0, factory);
    }
}

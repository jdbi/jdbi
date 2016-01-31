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
package org.jdbi.v3;

import static org.jdbi.v3.internal.JdbiStreams.toStream;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.ArgumentFactory;

class ArgumentRegistry
{

    static ArgumentRegistry copyOf(ArgumentRegistry registry) {
        return new ArgumentRegistry(registry.factories);
    }

    private final List<ArgumentFactory> factories = new CopyOnWriteArrayList<>();

    ArgumentRegistry()
    {
        factories.add(BuiltInArgumentFactory.INSTANCE);
    }

    ArgumentRegistry(List<ArgumentFactory> factories)
    {
        this.factories.addAll(factories);
    }

    Optional<Argument> findArgumentFor(Type expectedType, Object it, StatementContext ctx)
    {
        return Stream.concat(
                // Search first for factories accepting specific argument type
                factories.stream().flatMap(factory -> toStream(factory.build(expectedType, it, ctx))),
                // Fall back to any factory accepting Object if necessary
                factories.stream().flatMap(factory -> toStream(factory.build(Object.class, it, ctx))))
                .findFirst();
    }

    void register(ArgumentFactory argumentFactory)
    {
        factories.add(0, argumentFactory);
    }
}

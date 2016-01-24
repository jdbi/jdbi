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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.ArgumentFactory;

class Foreman
{

    private final List<ArgumentFactory> factories = new CopyOnWriteArrayList<>();

    Foreman()
    {
        factories.add(BuiltInArgumentFactory.INSTANCE);
    }

    Foreman(List<ArgumentFactory> factories)
    {
        this.factories.addAll(factories);
    }

    Argument waffle(Type expectedType, Object it, StatementContext ctx)
    {
        return Stream.concat(
                factories.stream()
                        .map(factory -> factory.build(expectedType, it, ctx))
                        .flatMap(this::toStream),
                // Fall back to any factory accepting Object if necessary but
                // prefer any more specific factory first.
                factories.stream()
                        .map(factory -> factory.build(Object.class, it, ctx))
                        .flatMap(this::toStream))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unbindable argument passed: " + it));
    }

    private <T> Stream<T> toStream(Optional<T> optional)
    {
        return optional.isPresent() ? Stream.of(optional.get()) : Stream.empty();
    }

    void register(ArgumentFactory argumentFactory)
    {
        factories.add(0, argumentFactory);
    }

    Foreman createChild()
    {
        return new Foreman(factories);
    }
}

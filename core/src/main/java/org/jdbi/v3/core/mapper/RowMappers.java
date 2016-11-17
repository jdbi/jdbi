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
package org.jdbi.v3.core.mapper;

import static org.jdbi.v3.core.internal.JdbiOptionals.findFirstPresent;
import static org.jdbi.v3.core.internal.JdbiStreams.toStream;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.core.JdbiConfig;
import org.jdbi.v3.core.Query;
import org.jdbi.v3.core.StatementContext;

public class RowMappers implements JdbiConfig<RowMappers> {
    private final Optional<RowMappers> parent;

    private final List<RowMapperFactory> factories = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Type, RowMapper<?>> cache = new ConcurrentHashMap<>();

    public RowMappers() {
        parent = Optional.empty();
    }

    private RowMappers(RowMappers that) {
        parent = Optional.of(that);
    }

    /**
     * Register a row mapper which will have its parameterized type inspected to determine what it maps to.
     * Will be used with {@link Query#mapTo(Class)} for registered mappings.
     *
     * The parameter must be concretely parameterized, we use the type argument T to
     * determine if it applies to a given type.
     *
     * @param mapper the row mapper
     * @throws UnsupportedOperationException if the RowMapper is not a concretely parameterized type
     * @return this
     */
    public RowMappers register(RowMapper<?> mapper)
    {
        return this.register(new InferredRowMapperFactory(mapper));
    }

    /**
     * Register a row mapper factory.
     *
     * Will be used with {@link Query#mapTo(Class)} for registered mappings.
     *
     * @param factory the row mapper factory
     * @return this
     */
    public RowMappers register(RowMapperFactory factory)
    {
        factories.add(0, factory);
        cache.clear();
        return this;
    }

    /**
     * Obtain a row mapper for the given type in the given context.
     *
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    public Optional<RowMapper<?>> findFor(Type type, StatementContext ctx) {
        // ConcurrentHashMap can enter an infinite loop on nested computeIfAbsent calls.
        // Since row mappers can decorate other row mappers, we have to populate the cache the old fashioned way.
        // See https://bugs.openjdk.java.net/browse/JDK-8062841, https://bugs.openjdk.java.net/browse/JDK-8142175
        RowMapper<?> cached = cache.get(type);

        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<RowMapper<?>> mapper = findFirstPresent(
                () -> factories.stream()
                        .flatMap(factory -> toStream(factory.build(type, ctx)))
                        .findFirst(),
                () -> ctx.findColumnMapperFor(type)
                        .map(SingleColumnMapper::new),
                () -> parent.flatMap(p -> p.findFor(type, ctx)));

        mapper.ifPresent(m -> cache.put(type, m));

        return mapper;
    }

    @Override
    public RowMappers createChild() {
        return new RowMappers(this);
    }
}

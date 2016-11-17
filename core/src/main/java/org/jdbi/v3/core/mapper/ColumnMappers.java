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
import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.array.SqlArrayMapperFactory;

public class ColumnMappers implements JdbiConfig<ColumnMappers> {
    private final Optional<ColumnMappers> parent;

    private final List<ColumnMapperFactory> factories = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Type, ColumnMapper<?>> cache = new ConcurrentHashMap<>();

    public ColumnMappers() {
        parent = Optional.empty();
        factories.add(new BuiltInMapperFactory());
        factories.add(new SqlArrayMapperFactory());
    }

    private ColumnMappers(ColumnMappers that) {
        parent = Optional.of(that);
    }

    /**
     * Register a column mapper which will have its parameterized type inspected to determine what it maps to.
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * The parameter must be concretely parameterized, we use the type argument T to
     * determine if it applies to a given type.
     *
     * @param mapper the column mapper
     * @throws UnsupportedOperationException if the ColumnMapper is not a concretely parameterized type
     * @return this
     */
    public ColumnMappers register(ColumnMapper<?> mapper)
    {
        return this.register(new InferredColumnMapperFactory(mapper));
    }

    /**
     * Register a column mapper factory.
     *
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param factory the column mapper factory
     * @return this
     */
    public ColumnMappers register(ColumnMapperFactory factory) {
        factories.add(0, factory);
        cache.clear();
        return this;
    }

    /**
     * Obtain a column mapper for the given type in the given context.
     *
     * @param type the target type to map to
     * @param ctx the statement context
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    public Optional<ColumnMapper<?>> findFor(Type type, StatementContext ctx) {
        // ConcurrentHashMap can enter an infinite loop on nested computeIfAbsent calls.
        // Since column mappers can decorate other column mappers, we have to populate the cache the old fashioned way.
        // See https://bugs.openjdk.java.net/browse/JDK-8062841, https://bugs.openjdk.java.net/browse/JDK-8142175
        ColumnMapper<?> cached = cache.get(type);

        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<ColumnMapper<?>> mapper = findFirstPresent(
                () -> factories.stream()
                        .flatMap(factory -> toStream(factory.build(type, ctx)))
                        .findFirst(),
                () -> parent.flatMap(p -> p.findFor(type, ctx)));

        mapper.ifPresent(m -> cache.put(type, m));

        return mapper;
    }

    @Override
    public ColumnMappers createChild() {
        return new ColumnMappers(this);
    }
}

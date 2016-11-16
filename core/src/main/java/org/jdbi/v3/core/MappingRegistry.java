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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.core.mapper.BuiltInMapperFactory;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.InferredColumnMapperFactory;
import org.jdbi.v3.core.mapper.InferredRowMapperFactory;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.util.SingleColumnMapper;

public class MappingRegistry implements JdbiConfig<MappingRegistry> {
    private final Optional<MappingRegistry> parent;

    private final List<RowMapperFactory> rowFactories = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Type, RowMapper<?>> rowCache = new ConcurrentHashMap<>();

    private final List<ColumnMapperFactory> columnFactories = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Type, ColumnMapper<?>> columnCache = new ConcurrentHashMap<>();

    public MappingRegistry() {
        parent = Optional.empty();
        columnFactories.add(new BuiltInMapperFactory());
        columnFactories.add(new SqlArrayMapperFactory());
    }

    private MappingRegistry(MappingRegistry that) {
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
    public MappingRegistry registerRowMapper(RowMapper<?> mapper)
    {
        return this.registerRowMapper(new InferredRowMapperFactory(mapper));
    }

    /**
     * Register a row mapper factory.
     *
     * Will be used with {@link Query#mapTo(Class)} for registered mappings.
     *
     * @param factory the row mapper factory
     * @return this
     */
    public MappingRegistry registerRowMapper(RowMapperFactory factory)
    {
        rowFactories.add(0, factory);
        rowCache.clear();
        return this;
    }

    /**
     * Obtain a row mapper for the given type in the given context.
     *
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    public Optional<RowMapper<?>> findRowMapperFor(Type type, StatementContext ctx) {
        // ConcurrentHashMap can enter an infinite loop on nested computeIfAbsent calls.
        // Since row mappers can decorate other row mappers, we have to populate the cache the old fashioned way.
        // See https://bugs.openjdk.java.net/browse/JDK-8062841, https://bugs.openjdk.java.net/browse/JDK-8142175
        RowMapper<?> cached = rowCache.get(type);

        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<RowMapper<?>> mapper = findFirstPresent(
                () -> rowFactories.stream()
                        .flatMap(factory -> toStream(factory.build(type, ctx)))
                        .findFirst(),
                () -> findColumnMapperFor(type, ctx)
                        .map(c -> new SingleColumnMapper<>(c)),
                () -> parent.flatMap(p -> p.findRowMapperFor(type, ctx)));

        mapper.ifPresent(m -> rowCache.put(type, m));

        return mapper;
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
    public MappingRegistry registerColumnMapper(ColumnMapper<?> mapper)
    {
        return this.registerColumnMapper(new InferredColumnMapperFactory(mapper));
    }

    /**
     * Register a column mapper factory.
     *
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param factory the column mapper factory
     * @return this
     */
    public MappingRegistry registerColumnMapper(ColumnMapperFactory factory) {
        columnFactories.add(0, factory);
        columnCache.clear();
        return this;
    }

    /**
     * Obtain a column mapper for the given type in the given context.
     *
     * @param type the target type to map to
     * @param ctx the statement context
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    public Optional<ColumnMapper<?>> findColumnMapperFor(Type type, StatementContext ctx) {
        // ConcurrentHashMap can enter an infinite loop on nested computeIfAbsent calls.
        // Since column mappers can decorate other column mappers, we have to populate the cache the old fashioned way.
        // See https://bugs.openjdk.java.net/browse/JDK-8062841, https://bugs.openjdk.java.net/browse/JDK-8142175
        ColumnMapper<?> cached = columnCache.get(type);

        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<ColumnMapper<?>> mapper = findFirstPresent(
                () -> columnFactories.stream()
                        .flatMap(factory -> toStream(factory.build(type, ctx)))
                        .findFirst(),
                () -> parent.flatMap(p -> p.findColumnMapperFor(type, ctx)));

        mapper.ifPresent(m -> columnCache.put(type, m));

        return mapper;
    }

    @Override
    public MappingRegistry createChild() {
        return new MappingRegistry(this);
    }
}

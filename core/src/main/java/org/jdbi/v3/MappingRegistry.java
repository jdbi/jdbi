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

import static org.jdbi.v3.internal.JdbiOptionals.findFirstPresent;
import static org.jdbi.v3.internal.JdbiStreams.toStream;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.tweak.ResultColumnMapper;
import org.jdbi.v3.tweak.ResultSetMapper;
import org.jdbi.v3.util.SingleColumnMapper;

class MappingRegistry
{
    private final List<ResultSetMapperFactory> rowFactories = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Type, ResultSetMapper<?>> rowCache = new ConcurrentHashMap<>();

    private final List<ResultColumnMapperFactory> columnFactories = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Type, ResultColumnMapper<?>> columnCache = new ConcurrentHashMap<>();

    MappingRegistry() {
        columnFactories.add(new BuiltInMapperFactory());
    }

    private MappingRegistry(MappingRegistry that) {
        rowFactories.addAll(that.rowFactories);
        rowCache.putAll(that.rowCache);
        columnFactories.addAll(that.columnFactories);
        columnCache.putAll(that.columnCache);
    }

    static MappingRegistry copyOf(MappingRegistry registry) {
        return new MappingRegistry(registry);
    }

    public void addMapper(ResultSetMapper<?> mapper)
    {
        this.addMapper(new InferredMapperFactory<>(mapper));
    }

    public void addMapper(ResultSetMapperFactory factory)
    {
        rowFactories.add(0, factory);
        rowCache.clear();
    }

    public Optional<ResultSetMapper<?>> findMapperFor(Type type, StatementContext ctx) {
        return Optional.ofNullable(rowCache.computeIfAbsent(type, t ->
                findFirstPresent(
                        () -> rowFactories.stream()
                                .flatMap(factory -> toStream(factory.build(t, ctx)))
                                .findFirst(),
                        () -> findColumnMapperFor(t, ctx)
                                .map(SingleColumnMapper::new))
                        .orElse(null)));
    }

    public void addColumnMapper(ResultColumnMapper<?> mapper)
    {
        this.addColumnMapper(new InferredColumnMapperFactory<>(mapper));
    }

    public void addColumnMapper(ResultColumnMapperFactory factory) {
        columnFactories.add(0, factory);
        columnCache.clear();
    }

    public Optional<ResultColumnMapper<?>> findColumnMapperFor(Type type, StatementContext ctx) {
        return Optional.ofNullable(columnCache.computeIfAbsent(type, t ->
                columnFactories.stream()
                        .flatMap(factory -> toStream(factory.build(t, ctx)))
                        .findFirst()
                        .orElse(null)));
    }
}

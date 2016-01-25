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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.internal.JdbiStreams;
import org.jdbi.v3.tweak.ResultColumnMapper;
import org.jdbi.v3.tweak.ResultSetMapper;
import org.jdbi.v3.util.SingleColumnMapper;

class MappingRegistry
{
    private static final PrimitivesColumnMapperFactory BUILT_INS = new PrimitivesColumnMapperFactory();

    private final List<ResultSetMapperFactory> rowFactories = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Type, ResultSetMapper<?>> rowCache = new ConcurrentHashMap<>();

    private final List<ResultColumnMapperFactory> columnFactories = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Type, ResultColumnMapper<?>> columnCache = new ConcurrentHashMap<>();

    static MappingRegistry copyOf(MappingRegistry parent) {
        MappingRegistry mr = new MappingRegistry();

        mr.rowFactories.addAll(parent.rowFactories);
        mr.rowCache.putAll(parent.rowCache);

        mr.columnFactories.addAll(parent.columnFactories);
        mr.columnCache.putAll(parent.columnCache);

        return mr;
    }

    public void addMapper(ResultSetMapper<?> mapper)
    {
        this.addMapper(new InferredMapperFactory<>(mapper));
    }

    public void addMapper(ResultSetMapperFactory factory)
    {
        rowFactories.add(factory);
        rowCache.clear();
    }

    public Optional<ResultSetMapper<?>> mapperFor(Type type, StatementContext ctx) {
        return Optional.ofNullable(rowCache.computeIfAbsent(type, t -> {
            Optional<ResultSetMapper<?>> mapper = rowFactories.stream()
                    .map(factory -> factory.build(type, ctx))
                    .flatMap(JdbiStreams::toStream)
                    .findFirst();
            if (mapper.isPresent()) {
                return mapper.get();
            }

            return columnMapperFor(type, ctx)
                    .map(SingleColumnMapper::new)
                    .orElse(null);
        }));
    }

    public void addColumnMapper(ResultColumnMapper<?> mapper)
    {
        this.addColumnMapper(new InferredColumnMapperFactory<>(mapper));
    }

    public void addColumnMapper(ResultColumnMapperFactory factory) {
        columnFactories.add(factory);
        columnCache.clear();
    }

    public Optional<ResultColumnMapper<?>> columnMapperFor(Type type, StatementContext ctx) {
        return Optional.ofNullable(columnCache.computeIfAbsent(type, t -> {
            Optional<ResultColumnMapper<?>> mapper = columnFactories.stream()
                    .map(factory -> factory.build(t, ctx))
                    .flatMap(JdbiStreams::toStream)
                    .findFirst();
            if (mapper.isPresent()) {
                return mapper.get();
            }

            return BUILT_INS.build(type, ctx).orElse(null);
        }));
    }
}

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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.tweak.ResultColumnMapper;
import org.jdbi.v3.tweak.ResultSetMapper;
import org.jdbi.v3.util.SingleColumnMapper;

class MappingRegistry
{
    private static final PrimitivesColumnMapperFactory BUILT_INS = new PrimitivesColumnMapperFactory();

    private final List<ResultSetMapperFactory> rowFactories = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Class<?>, ResultSetMapper<?>> rowCache = new ConcurrentHashMap<>();

    private final List<ResultColumnMapperFactory> columnFactories = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Class<?>, ResultColumnMapper<?>> columnCache = new ConcurrentHashMap<>();

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

    @SuppressWarnings("unchecked")
    public <T> ResultSetMapper<? extends T> mapperFor(Class<T> type, StatementContext ctx) {
        ResultSetMapper<? extends T> mapper = (ResultSetMapper<? extends T>) rowCache.get(type);
        if (mapper != null) {
            return mapper;
        }

        for (ResultSetMapperFactory factory : rowFactories) {
            if (factory.accepts(type, ctx)) {
                mapper = factory.mapperFor(type, ctx);
                rowCache.put(type, mapper);
                return mapper;
            }
        }

        ResultColumnMapper<? extends T> columnMapper = columnMapperFor(type, ctx);
        if (columnMapper != null) {
            mapper = new SingleColumnMapper<>(columnMapper);
            rowCache.put(type, mapper);
            return mapper;
        }

        throw new UnsupportedOperationException("No mapper registered for " + type.getName());
    }

    public void addColumnMapper(ResultColumnMapper<?> mapper)
    {
        this.addColumnMapper(new InferredColumnMapperFactory<>(mapper));
    }

    public void addColumnMapper(ResultColumnMapperFactory factory) {
        columnFactories.add(factory);
        columnCache.clear();
    }

    @SuppressWarnings("unchecked")
    public <T> ResultColumnMapper<T> columnMapperFor(Class<T> type, StatementContext ctx) {
        ResultColumnMapper<?> mapper = columnCache.get(type);
        if (mapper != null) {
            return (ResultColumnMapper<T>) mapper;
        }

        for (ResultColumnMapperFactory factory : columnFactories) {
            if (factory.accepts(type, ctx)) {
                mapper = factory.columnMapperFor(type, ctx);
                columnCache.put(type, mapper);
                return (ResultColumnMapper<T>) mapper;
            }
        }

        if (BUILT_INS.accepts(type, ctx)) {
            mapper = BUILT_INS.columnMapperFor(type, ctx);
            columnCache.put(type, mapper);
            return (ResultColumnMapper<T>) mapper;
        }

        return null;
    }
}

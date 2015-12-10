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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.DBIException;
import org.skife.jdbi.v2.tweak.ResultColumnMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.SingleColumnMapper;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class MappingRegistry
{
    private static final PrimitivesColumnMapperFactory BUILT_INS = new PrimitivesColumnMapperFactory();

    private final List<ResultSetMapperFactory> rowFactories = new CopyOnWriteArrayList<ResultSetMapperFactory>();
    private final ConcurrentHashMap<Class, ResultSetMapper> rowCache = new ConcurrentHashMap<Class, ResultSetMapper>();

    private final List<ResultColumnMapperFactory> columnFactories = new CopyOnWriteArrayList<ResultColumnMapperFactory>();
    private final ConcurrentHashMap<Class, ResultColumnMapper> columnCache = new ConcurrentHashMap<Class, ResultColumnMapper>();

    /**
     * Copy Constructor
     */
    MappingRegistry(MappingRegistry parent)
    {
        rowFactories.addAll(parent.rowFactories);
        rowCache.putAll(parent.rowCache);

        columnFactories.addAll(parent.columnFactories);
        columnCache.putAll(parent.columnCache);
    }

    MappingRegistry() {

    }

    public void addMapper(ResultSetMapper mapper)
    {
        this.addMapper(new InferredMapperFactory(mapper));
    }

    public void addMapper(ResultSetMapperFactory factory)
    {
        rowFactories.add(factory);
        rowCache.clear();
    }

    public ResultSetMapper<?> mapperFor(Class<?> type, StatementContext ctx) {
        ResultSetMapper<?> mapper = rowCache.get(type);
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

        ResultColumnMapper columnMapper = columnMapperFor(type, ctx);
        if (columnMapper != null) {
            mapper = new SingleColumnMapper(columnMapper);
            rowCache.put(type, mapper);
            return mapper;
        }

        throw new DBIException("No mapper registered for " + type.getName()) {};
    }

    public void addColumnMapper(ResultColumnMapper mapper)
    {
        this.addColumnMapper(new InferredColumnMapperFactory(mapper));
    }

    public void addColumnMapper(ResultColumnMapperFactory factory) {
        columnFactories.add(factory);
        columnCache.clear();
    }

    public ResultColumnMapper<?> columnMapperFor(Class<?> type, StatementContext ctx) {
        ResultColumnMapper<?> mapper = columnCache.get(type);
        if (mapper != null) {
            return mapper;
        }

        for (ResultColumnMapperFactory factory : columnFactories) {
            if (factory.accepts(type, ctx)) {
                mapper = factory.columnMapperFor(type, ctx);
                columnCache.put(type, mapper);
                return mapper;
            }
        }

        if (BUILT_INS.accepts(type, ctx)) {
            mapper = BUILT_INS.columnMapperFor(type, ctx);
            columnCache.put(type, mapper);
            return mapper;
        }

        return null;
    }
}

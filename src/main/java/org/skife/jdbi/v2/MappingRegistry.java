/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class MappingRegistry
{
    private static final PrimitivesMapperFactory BUILT_IN_MAPPERS = new PrimitivesMapperFactory();

    private final List<ResultSetMapperFactory> rowFactories = new CopyOnWriteArrayList<ResultSetMapperFactory>();
    private final ConcurrentHashMap<Class, ResultSetMapper> rowCache = new ConcurrentHashMap<Class, ResultSetMapper>();

    private final List<ResultColumnMapperFactory> columnFactories = new CopyOnWriteArrayList<ResultColumnMapperFactory>();
    private final ConcurrentHashMap<Class, ResultColumnMapper> columnCache = new ConcurrentHashMap<Class, ResultColumnMapper>();

    /**
     * Copy Constructor
     */
    public MappingRegistry(MappingRegistry parent)
    {
        rowFactories.addAll(parent.rowFactories);
        rowCache.putAll(parent.rowCache);

        columnFactories.addAll(parent.columnFactories);
        columnCache.putAll(parent.columnCache);
    }

    public MappingRegistry() {

    }

    public void add(ResultSetMapper mapper)
    {
        this.add(new InferredMapperFactory(mapper));
        if (mapper instanceof ResultColumnMapper) {
            this.addColumn((ResultColumnMapper) mapper);
        }
    }

    public void add(ResultSetMapperFactory factory)
    {
        rowFactories.add(factory);
        rowCache.clear();

        if (factory instanceof ResultColumnMapperFactory) {
            this.addColumn((ResultColumnMapperFactory) factory);
        }
    }

    public ResultSetMapper mapperFor(Class type, StatementContext ctx) {
        if (rowCache.containsKey(type)) {
            ResultSetMapper mapper = rowCache.get(type);
            if (mapper != null) {
                return mapper;
            }
        }

        for (ResultSetMapperFactory factory : rowFactories) {
            if (factory.accepts(type, ctx)) {
                ResultSetMapper mapper =  factory.mapperFor(type, ctx);
                rowCache.put(type, mapper);
                return mapper;
            }
        }

        if (BUILT_IN_MAPPERS.accepts(type, ctx)) {
            ResultSetMapper mapper = BUILT_IN_MAPPERS.mapperFor(type, ctx);
            rowCache.put(type, mapper);
            return mapper;
        }

        throw new DBIException("No mapper registered for " + type.getName()) {};
    }

    public void addColumn(ResultColumnMapper mapper)
    {
        this.addColumn(new InferredColumnMapperFactory(mapper));
    }

    public void addColumn(ResultColumnMapperFactory factory) {
        columnFactories.add(factory);
        columnCache.clear();
    }

    public ResultColumnMapper columnMapperFor(Class type, StatementContext ctx) {
        if (columnCache.containsKey(type)) {
            ResultColumnMapper mapper = columnCache.get(type);
            if (mapper != null) {
                return mapper;
            }
        }

        for (ResultColumnMapperFactory factory : columnFactories) {
            if (factory.accepts(type, ctx)) {
                ResultColumnMapper mapper =  factory.columnMapperFor(type, ctx);
                columnCache.put(type, mapper);
                return mapper;
            }
        }

        if (BUILT_IN_MAPPERS.accepts(type, ctx)) {
            ResultColumnMapper mapper = BUILT_IN_MAPPERS.columnMapperFor(type, ctx);
            columnCache.put(type, mapper);
            return mapper;
        }

        throw new DBIException("No column mapper registered for " + type.getName()) {};
    }
}

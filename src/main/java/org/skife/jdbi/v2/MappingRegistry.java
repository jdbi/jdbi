/*
 * Copyright (C) 2004 - 2013 Brian McCallister
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
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MappingRegistry
{
    private static final PrimitivesMapperFactory BUILT_IN_MAPPERS = new PrimitivesMapperFactory();

    private final List<ResultSetMapperFactory> factories = new CopyOnWriteArrayList<ResultSetMapperFactory>();
    private final ConcurrentHashMap<Class, ResultSetMapper> cache = new ConcurrentHashMap<Class, ResultSetMapper>();

    /**
     * Copy Constructor
     */
    public MappingRegistry(MappingRegistry parent)
    {
        factories.addAll(parent.factories);
        cache.putAll(parent.cache);
    }

    public MappingRegistry() {

    }

    public void add(ResultSetMapper mapper)
    {
        this.add(new InferredMapperFactory(mapper));
    }

    public void add(ResultSetMapperFactory factory)
    {
        factories.add(factory);
        cache.clear();
    }

    public ResultSetMapper mapperFor(Class type, StatementContext ctx) {
		return mapperFor(type, ctx, true);
	}

	public ResultSetMapper mapperFor(Class type, StatementContext ctx, boolean shouldThrow) {
        if (cache.containsKey(type)) {
            ResultSetMapper mapper = cache.get(type);
            if (mapper != null) {
                return mapper;
            }
        }

        for (ResultSetMapperFactory factory : factories) {
            if (factory.accepts(type, ctx)) {
                ResultSetMapper mapper =  factory.mapperFor(type, ctx);
                cache.put(type, mapper);
                return mapper;
            }
        }

        if (BUILT_IN_MAPPERS.accepts(type, ctx)) {
            ResultSetMapper mapper = BUILT_IN_MAPPERS.mapperFor(type, ctx);
            cache.put(type, mapper);
            return mapper;
        }

        if (shouldThrow) throw new DBIException("No mapper registered for " + type.getName()) {};
        else return null;
    }
}

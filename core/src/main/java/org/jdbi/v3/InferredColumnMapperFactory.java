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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;

import org.jdbi.v3.tweak.ResultColumnMapper;

class InferredColumnMapperFactory<X> implements ResultColumnMapperFactory
{
    private static final TypeResolver tr = new TypeResolver();
    private final Class<X> maps;
    private final ResultColumnMapper<X> mapper;

    @SuppressWarnings("unchecked")
    InferredColumnMapperFactory(ResultColumnMapper<X> mapper)
    {
        this.mapper = mapper;
        ResolvedType rt = tr.resolve(mapper.getClass());
        List<ResolvedType> rs = rt.typeParametersFor(ResultColumnMapper.class);
        if (rs.isEmpty() || rs.get(0).getErasedType().equals(Object.class)) {
            throw new UnsupportedOperationException("Must use a concretely typed ResultColumnMapper here");
        }

        maps = (Class<X>) rs.get(0).getErasedType();
    }

    @Override
    public boolean accepts(Class<?> type, StatementContext ctx)
    {
        return maps.equals(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ResultColumnMapper<? extends T> columnMapperFor(Class<T> type, StatementContext ctx)
    {
        return (ResultColumnMapper<? extends T>) mapper;
    }
}

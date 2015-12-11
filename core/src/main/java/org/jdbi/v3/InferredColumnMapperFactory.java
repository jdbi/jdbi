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
    private final ResolvedType maps;
    private final ResultColumnMapper<X> mapper;

    @SuppressWarnings("unchecked")
    InferredColumnMapperFactory(ResultColumnMapper<X> mapper)
    {
        List<ResolvedType> typeParameters = new TypeResolver().resolve(mapper.getClass())
                .typeParametersFor(ResultColumnMapper.class);
        if (typeParameters.isEmpty() || typeParameters.get(0).getErasedType().equals(Object.class)) {
            throw new UnsupportedOperationException("Must use a concretely typed ResultColumnMapper here");
        }
        this.maps = typeParameters.get(0);
        this.mapper = mapper;
    }

    @Override
    public boolean accepts(ResolvedType type, StatementContext ctx)
    {
        return maps.equals(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ResultColumnMapper<? extends T> columnMapperFor(ResolvedType type, StatementContext ctx)
    {
        return (ResultColumnMapper<? extends T>) mapper;
    }
}

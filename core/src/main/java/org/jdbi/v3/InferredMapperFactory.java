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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.jdbi.v3.tweak.ResultSetMapper;

import java.util.List;

class InferredMapperFactory<X> implements ResultSetMapperFactory
{
    private final ResolvedType maps;
    private final ResultSetMapper<X> mapper;

    InferredMapperFactory(ResultSetMapper<X> mapper)
    {
        List<ResolvedType> typeParameters = new TypeResolver().resolve(mapper.getClass())
                .typeParametersFor(ResultSetMapper.class);
        if (typeParameters.isEmpty() || typeParameters.get(0).getErasedType().equals(Object.class)) {
          throw new UnsupportedOperationException("Must use a concretely typed ResultSetMapper here");
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
    public <T> ResultSetMapper<? extends T> mapperFor(ResolvedType type, StatementContext ctx)
    {
        if (!type.equals(maps)) {
            throw new IllegalArgumentException("Expected to map " + type + " but I only map " + maps);
        }
        return (ResultSetMapper<? extends T>) mapper;
    }
}

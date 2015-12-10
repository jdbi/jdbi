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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.util.List;

class InferredMapperFactory implements ResultSetMapperFactory
{
    private static final TypeResolver tr = new TypeResolver();
    private final Class maps;
    private final ResultSetMapper mapper;

    InferredMapperFactory(ResultSetMapper mapper)
    {
        this.mapper = mapper;
        ResolvedType rt = tr.resolve(mapper.getClass());
        List<ResolvedType> rs = rt.typeParametersFor(ResultSetMapper.class);
        if (rs.isEmpty() || rs.get(0).getErasedType().equals(Object.class)) {
            throw new UnsupportedOperationException("Must use a concretely typed ResultSetMapper here");
        }

        maps = rs.get(0).getErasedType();
    }

    @Override
    public boolean accepts(Class type, StatementContext ctx)
    {
        return maps.equals(type);
    }

    @Override
    public ResultSetMapper mapperFor(Class type, StatementContext ctx)
    {
        return mapper;
    }
}

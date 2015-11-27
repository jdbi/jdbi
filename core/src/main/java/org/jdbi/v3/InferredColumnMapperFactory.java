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

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

import com.google.common.reflect.TypeToken;
import org.jdbi.v3.tweak.ResultColumnMapper;

class InferredColumnMapperFactory<X> implements ResultColumnMapperFactory
{
    private final TypeToken<X> maps;
    private final ResultColumnMapper<X> mapper;

    @SuppressWarnings("unchecked")
    InferredColumnMapperFactory(ResultColumnMapper<X> mapper)
    {
        this.maps = Arrays.stream(mapper.getClass().getGenericInterfaces())
                .filter(type -> type instanceof ParameterizedType)
                .map(type -> (ParameterizedType) type)
                .filter(type -> type.getRawType().equals(ResultColumnMapper.class))
                .map(type -> type.getActualTypeArguments()[0])
                .filter(type -> type instanceof Class)
                .map(type -> (TypeToken<X>) TypeToken.of(type))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Must use a concretely typed ResultSetMapper here"));
        this.mapper = mapper;
    }

    @Override
    public boolean accepts(TypeToken<?> type, StatementContext ctx)
    {
        return maps.equals(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ResultColumnMapper<? extends T> columnMapperFor(TypeToken<T> type, StatementContext ctx)
    {
        return (ResultColumnMapper<? extends T>) mapper;
    }
}

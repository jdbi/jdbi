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

import static org.jdbi.v3.Types.findGenericParameter;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.tweak.ColumnMapper;

class InferredColumnMapperFactory implements ColumnMapperFactory
{
    private final Type maps;
    private final ColumnMapper<?> mapper;

    InferredColumnMapperFactory(ColumnMapper<?> mapper)
    {
        this.maps = findGenericParameter(mapper.getClass(), ColumnMapper.class)
                .orElseThrow(() -> new UnsupportedOperationException("Must use a concretely typed ColumnMapper here"));
        this.mapper = mapper;
    }

    @Override
    public Optional<ColumnMapper<?>> build(Type type, StatementContext ctx) {
        return maps.equals(type)
                ? Optional.of(mapper)
                : Optional.empty();
    }
}

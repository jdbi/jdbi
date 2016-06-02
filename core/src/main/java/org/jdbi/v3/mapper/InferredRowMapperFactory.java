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
package org.jdbi.v3.mapper;

import static org.jdbi.v3.Types.findGenericParameter;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.StatementContext;

/**
 * A generic RowMapperFactory that reflectively inspects a
 * {@code RowMapper<T>} and maps only to columns of type
 * {@code T}.  The type parameter T must be accessible
 * via reflection or an {@link UnsupportedOperationException}
 * will be thrown.
 */
public class InferredRowMapperFactory implements RowMapperFactory
{
    private final Type maps;
    private final RowMapper<?> mapper;

    public InferredRowMapperFactory(RowMapper<?> mapper)
    {
        this.maps = findGenericParameter(mapper.getClass(), RowMapper.class)
                .orElseThrow(() -> new UnsupportedOperationException("Must use a concretely typed RowMapper here"));
        this.mapper = mapper;
    }

    @Override
    public Optional<RowMapper<?>> build(Type type, StatementContext ctx) {
        return maps.equals(type)
                ? Optional.of(mapper)
                : Optional.empty();
    }
}

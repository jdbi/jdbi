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

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.StatementContext;

/**
 * Factory interface used to produce column mappers.
 */
@FunctionalInterface
public interface ColumnMapperFactory
{
    /**
     * Supplies a column mapper which will map columns to type if the factory supports it; empty otherwise.
     *
     * @param type the target type to map to
     * @param ctx the statement context.
     * @return a column mapper for the given type if this factory supports it, or <code>Optional.empty()</code> otherwise.
     * @see StatementContext#findColumnMapperFor(Type)
     */
    Optional<ColumnMapper<?>> build(Type type, StatementContext ctx);
}

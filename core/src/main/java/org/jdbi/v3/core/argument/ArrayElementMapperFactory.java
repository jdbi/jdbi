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
package org.jdbi.v3.core.argument;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.core.StatementContext;

/**
 * Factory interface to produce array element mappers.
 */
@FunctionalInterface
public interface ArrayElementMapperFactory {
    /**
     * Supplies an array element mapper which will map array elements from {@code type} to a driver-supported type,
     * if the factory supports it; empty otherwise.
     *
     * @param type the element type to map from
     * @param ctx  the statement context.
     * @return an array element mapper for the given element type if this factory supports it, or
     * <code>Optional.empty()</code> otherwise.
     * @see StatementContext#findColumnMapperFor(Type)
     */
    Optional<ArrayElementMapper<?>> build(Type type, StatementContext ctx);
}

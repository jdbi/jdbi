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
 * Factory interface to produce {@link SqlArrayType} instances.
 */
@FunctionalInterface
public interface SqlArrayTypeFactory {
    /**
     * Returns an {@link SqlArrayType} for the given {@code elementType} if this factory supports it; empty otherwise.
     *
     * @param elementType the array element type
     * @param ctx the statement context.
     * @return an {@link SqlArrayType} for the given {@code elementType} if this factory supports it; empty otherwise.
     * @see StatementContext#findArrayTypeFor(Type)
     */
    Optional<SqlArrayType<?>> build(Type elementType, StatementContext ctx);
}

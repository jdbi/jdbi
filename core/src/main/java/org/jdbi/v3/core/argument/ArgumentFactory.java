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

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;

@FunctionalInterface
public interface ArgumentFactory
{
    /**
     * Returns an {@link Argument} for the given value if the factory supports it; empty otherwise.
     *
     * @param type  the known type of value. Depending on the situation this may be a full generic signature e.g.
     *              {@link java.lang.reflect.ParameterizedType}, a {@link Class}, or Object.class if no type information
     *              is known.
     * @param value the value to convert into an {@link Argument}
     * @param config the config registry, for composition
     * @return an argument for the given value if this factory supports it, or <code>Optional.empty()</code> otherwise.
     * @see ConfigRegistry#findArgumentFor(Type, Object) for composeable argument types.
     * @see StatementContext#findArgumentFor(Type, Object)
     * @see Arguments#findFor(Type, Object)
     */
    Optional<Argument> build(Type type, Object value, ConfigRegistry config);
}

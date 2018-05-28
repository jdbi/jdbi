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

import java.util.Optional;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Inspect a value with optional static type information and produce
 * an {@link Argument} that binds the value to a prepared statement.
 */
@FunctionalInterface
public interface QualifiedArgumentFactory {
    /**
     * Returns an {@link Argument} for the given value of the qualified type, if the factory supports it; empty otherwise.
     *
     * @param type  the qualified type of value. A qualified type consists of a type (whether Class or generic type) with
     *              a set of qualifier objects.
     * @param value the value to convert into an {@link Argument}
     * @param config the config registry, for composition
     * @return an argument for the given value if this factory supports it, or <code>Optional.empty()</code> otherwise.
     * @see StatementContext#findArgumentFor(QualifiedType, Object)
     * @see Arguments#findFor(QualifiedType, Object)
     */
    Optional<Argument> build(QualifiedType type, Object value, ConfigRegistry config);
}

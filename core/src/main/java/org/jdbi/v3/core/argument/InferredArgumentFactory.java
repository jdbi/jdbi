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

import static org.jdbi.v3.core.util.GenericTypes.findGenericParameter;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.core.ConfigRegistry;

/**
 * A generic ArgumentFactory that reflectively inspects an
 * {@code Argument<T>} and binds only arguments of type
 * {@code T}.  The type parameter T must be accessible
 * via reflection or an {@link UnsupportedOperationException}
 * will be thrown.
 */
class InferredArgumentFactory implements ArgumentFactory {
    private final Type binds;
    private final Argument<?> argument;

    InferredArgumentFactory(Argument<?> argument) {
        this.binds = findGenericParameter(argument.getClass(), Argument.class)
                .orElseThrow(() -> new UnsupportedOperationException("Must use a concretely typed Argument here"));
        this.argument = argument;
    }

    @Override
    public Optional<Argument<?>> build(Type type, ConfigRegistry config) {
        return binds.equals(type)
                ? Optional.of(argument)
                : Optional.empty();
    }
}

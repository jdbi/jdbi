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
package org.jdbi.v3.core.statement;

import java.util.Objects;
import java.util.Optional;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.NullArgument;
import org.jdbi.v3.meta.Beta;

/**
 * Selects the mode of operation of the statement {@code defineNamedBindings} feature.
 */
@Beta
public enum DefineNamedBindingMode {
    /**
     * Bind a boolean {@true} for present values, {@false} for null values.
     * Default behavior.
     */
    BOOLEAN {
        @Override
        Optional<Object> apply(Argument arg) {
            return Optional.of(arg instanceof NullArgument ? false : true);
        }
    },
    /**
     * Bind the Argument's {@code toString} value as the template parameter.
     * Please be cautious not to allow SQL injection!
     */
    TO_STRING {
        @Override
        Optional<Object> apply(Argument arg) {
            return Optional.ofNullable(Objects.toString(arg));
        }
    },
    ;

    abstract Optional<Object> apply(Argument arg);
}

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
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Optional;
import org.jdbi.v3.core.config.ConfigRegistry;

class EnumArgumentFactory implements ArgumentFactory {
    private static final ArgBuilder<String> STR_BUILDER = v -> new BuiltInArgument<>(String.class, Types.VARCHAR, PreparedStatement::setString, v);

    @Override
    public Optional<Argument> build(Type expectedType, Object value, ConfigRegistry config) {
        // Enums must be bound as VARCHAR
        // TODO use the same configuration as EnumMapperFactory for consistency
        if (value instanceof Enum) {
            Enum<?> enumValue = (Enum<?>) value;
            return Optional.of(STR_BUILDER.build(enumValue.name()));
        } else {
            return Optional.empty();
        }
    }
}

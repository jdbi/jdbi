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
import java.sql.Types;
import java.util.Optional;
import org.jdbi.v3.core.EnumByName;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.internal.Enums;

@EnumByName
class EnumByNameArgumentFactory implements ArgumentFactory {
    @Override
    public Optional<Argument> build(Type type, Object rawValue, ConfigRegistry config) {
        if (!Enums.isEnum(type)) {
            return Optional.empty();
        }

        if (rawValue == null) {
            return Optional.of(new NullArgument(Types.VARCHAR));
        }

        Enum<?> enumValue = (Enum<?>) rawValue;
        return config.get(Arguments.class).findFor(String.class, enumValue.name());
    }
}

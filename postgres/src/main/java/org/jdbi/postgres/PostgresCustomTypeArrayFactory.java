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
package org.jdbi.postgres;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.core.array.SqlArrayType;
import org.jdbi.core.array.SqlArrayTypeFactory;
import org.jdbi.core.config.ConfigView;
import org.jdbi.core.generic.GenericTypes;

/**
 * Binds arrays of Postgres {@linkplain PostgresTypes#registerCustomType custom types} by looking up the
 * element type's registered SQL type name in {@link PostgresTypes}. A single instance handles every registered
 * custom type, so registering a custom type is a pure update to {@link PostgresTypes} rather than a write into
 * {@link org.jdbi.core.array.SqlArrayTypes}.
 */
final class PostgresCustomTypeArrayFactory implements SqlArrayTypeFactory {
    @Override
    public Optional<SqlArrayType<?>> build(Type elementType, ConfigView config) {
        final String typeName = config.get(PostgresTypes.class).sqlArrayTypeName(GenericTypes.getErasedType(elementType));
        return Optional.ofNullable(typeName).map(name -> SqlArrayType.of(name, Function.identity()));
    }
}

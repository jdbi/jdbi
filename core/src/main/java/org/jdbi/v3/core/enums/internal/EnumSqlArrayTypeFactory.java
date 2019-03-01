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
package org.jdbi.v3.core.enums.internal;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.core.array.SqlArrayType;
import org.jdbi.v3.core.array.SqlArrayTypeFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.enums.EnumStrategy;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.internal.EnumStrategies;
import org.jdbi.v3.core.qualifier.QualifiedType;

// TODO Make this a QualifiedSqlArrayTypeFactory after we add qualified SQL array support
public class EnumSqlArrayTypeFactory implements SqlArrayTypeFactory {
    @Override
    @SuppressWarnings("unchecked")
    public Optional<SqlArrayType<?>> build(Type elementType, ConfigRegistry config) {
        return Optional.of(elementType)
            .map(GenericTypes::getErasedType)
            .filter(Class::isEnum)
            .map(clazz -> makeSqlArrayType((Class<Enum>) clazz, config));
    }

    private <E extends Enum<E>> SqlArrayType<E> makeSqlArrayType(Class<E> enumClass, ConfigRegistry config) {
        boolean byName = EnumStrategy.BY_NAME == config.get(EnumStrategies.class).findStrategy(QualifiedType.of(enumClass));

        return byName
            ? SqlArrayType.of("varchar", Enum::name)
            : SqlArrayType.of("integer", Enum::ordinal);
    }
}

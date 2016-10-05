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
package org.jdbi.v3.postgres;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.util.GenericTypes;

import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.Optional;

public class EnumSetMapperFactory implements ColumnMapperFactory {

    @Override
    @SuppressWarnings("unchecked")
    public Optional<ColumnMapper<?>> build(Type type, StatementContext ctx) {
        Class<?> erasedType = GenericTypes.getErasedType(type);
        if (erasedType == EnumSet.class) {
            Optional<Type> genericParameter = GenericTypes.findGenericParameter(type, EnumSet.class);
            Type type1 = genericParameter
                    .orElseThrow(() -> new IllegalArgumentException("No generic information for " + type));
            if (Enum.class.isAssignableFrom((Class<?>) type1)) {
                return Optional.of(new EnumSetColumnMapper<>((Class<Enum>) type1));
            } else {
                throw new IllegalArgumentException("Generic type is not enum");
            }
        }
        return Optional.empty();
    }

}

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
package org.jdbi.v3.postgres.internal;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;

public class BitStringEnumSetArgumentFactory implements ArgumentFactory.Preparable {
    @Override
    public Optional<Function<Object, Argument>> prepare(Type type, ConfigRegistry config) {
        if (!EnumSet.class.isAssignableFrom(GenericTypes.getErasedType(type))) {
            return Optional.empty();
        }

        return Optional.of(buildGeneric(type));
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> Function<Object, Argument> buildGeneric(Type type) {
        Class<E> enumType = GenericTypes.findGenericParameter(type, EnumSet.class)
            // guaranteed by EnumSet
            .map(t -> (Class<E>) t)
            .orElseThrow(() -> new IllegalArgumentException("No generic type information for " + type));

        return value -> new BitStringEnumSetArgument<>(enumType, (EnumSet<E>) value);
    }

    @Override
    public Collection<? extends Type> prePreparedTypes() {
        return Collections.singleton(EnumSet.class);
    }
}

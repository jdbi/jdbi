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
package org.jdbi.v3.core.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import org.jdbi.v3.core.EnumByName;
import org.jdbi.v3.core.EnumByOrdinal;
import org.jdbi.v3.core.EnumConfig;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;

public class Enums {
    private Enums() {
        throw new UtilityClassException();
    }

    // all enums are T in Enum<T>
    public static boolean isEnum(Type type) {
        return GenericTypes.getErasedType(type).isEnum();
    }

    public static QualifiedType qualifyByNameOrOrdinal(Type type, ConfigRegistry config) {
        if (!isEnum(type)) {
            throw new IllegalArgumentException("type " + type.getTypeName() + " is not an Enum");
        }

        // Ts of Enum<T> can only be classes
        @SuppressWarnings("unchecked")
        Class<? extends Enum<?>> enumClass = (Class) type;
        Annotation qualifier = getAppropriateQualifier(enumClass, config);

        return QualifiedType.of(enumClass).with(qualifier);
    }

    private static Annotation getAppropriateQualifier(Class<? extends Enum> enumClass, ConfigRegistry config) {
        Class<? extends Annotation> qualifier = classByNameOrOrdinal(enumClass);

        if (qualifier == null) {
            qualifier = config.get(EnumConfig.class).getEnumHandling();
        }

        return AnnotationFactory.create(qualifier);
    }

    private static Class<? extends Annotation> classByNameOrOrdinal(Class<? extends Enum> enumClass) {
        Set<Annotation> onClass = Qualifiers.getQualifiers(enumClass);

        boolean byNameOnClass = onClass.stream().anyMatch(EnumByName.class::isInstance);
        boolean byOrdinalOnClass = onClass.stream().anyMatch(EnumByOrdinal.class::isInstance);

        if (byNameOnClass && byOrdinalOnClass) {
            throw new IllegalArgumentException(String.format(
                "enum %s is annotated with both @%s and @%s",
                enumClass.getName(),
                EnumByName.class.getSimpleName(),
                EnumByOrdinal.class.getSimpleName()
            ));
        } else if (byNameOnClass) {
            return EnumByName.class;
        } else if (byOrdinalOnClass) {
            return EnumByOrdinal.class;
        } else {
            return null;
        }
    }
}

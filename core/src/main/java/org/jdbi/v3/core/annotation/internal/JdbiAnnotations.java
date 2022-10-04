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
package org.jdbi.v3.core.annotation.internal;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.jdbi.v3.core.annotation.JdbiProperty;
import org.jdbi.v3.core.internal.UtilityClassException;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoProperty;

public final class JdbiAnnotations {
    private JdbiAnnotations() {
        throw new UtilityClassException();
    }

    public static boolean isMapped(AnnotatedElement element) {
        return isMapped(jdbiProperty(element));
    }

    public static boolean isBound(AnnotatedElement element) {
        return isBound(jdbiProperty(element));
    }

    public static boolean isMapped(PojoProperty<?> property) {
        return isMapped(property.getAnnotation(JdbiProperty.class));
    }

    public static boolean isBound(PojoProperty<?> property) {
        return isBound(property.getAnnotation(JdbiProperty.class));
    }

    private static boolean isBound(Optional<JdbiProperty> annotation) {
        return annotation.map(JdbiProperty::bind).orElse(true);
    }

    private static boolean isMapped(Optional<JdbiProperty> annotation) {
        return annotation.map(JdbiProperty::map).orElse(true);
    }

    private static Optional<JdbiProperty> jdbiProperty(AnnotatedElement element) {
        return Optional.ofNullable(element.getAnnotation(JdbiProperty.class));
    }
}

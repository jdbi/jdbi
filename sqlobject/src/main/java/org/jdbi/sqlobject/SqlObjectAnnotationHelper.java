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
package org.jdbi.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.jdbi.core.extension.annotation.UseExtensionHandler;
import org.jdbi.core.internal.UtilityClassException;

import static org.jdbi.sqlobject.SqlObjectFactory.EXTENSION_ID;

final class SqlObjectAnnotationHelper {

    private SqlObjectAnnotationHelper() {
        throw new UtilityClassException();
    }

    static boolean matchSqlAnnotations(Annotation a) {
        return matchNewAnnotation(a) || matchOldAnnotation(a);
    }

    static List<Class<?>> findSqlMethodAnnotations(Method method) {
        return findOldAnnotations(method)
                .toList();
    }

    private static boolean matchNewAnnotation(Annotation a) {
        UseExtensionHandler extensionHandlerAnnotation = a.annotationType().getAnnotation(UseExtensionHandler.class);
        return extensionHandlerAnnotation != null && EXTENSION_ID.equals(extensionHandlerAnnotation.id());
    }

    private static boolean matchOldAnnotation(Annotation a) {
        return a.annotationType().isAnnotationPresent(SqlOperation.class);
    }

    static Stream<Class<?>> findOldAnnotations(Method method) {
        return Stream.of(method.getAnnotations())
                .filter(SqlObjectAnnotationHelper::matchOldAnnotation)
                .map(Annotation::annotationType);
    }

    static <T extends Annotation> Optional<T> findAnnotation(Class<T> annotationClass, AnnotatedElement... elements) {
        return Arrays.stream(elements)
                .map(e -> e.getAnnotation(annotationClass))
                .filter(Objects::nonNull)
                .findFirst();
    }
}

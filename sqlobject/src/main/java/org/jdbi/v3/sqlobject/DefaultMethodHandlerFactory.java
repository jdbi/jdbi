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
package org.jdbi.v3.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.stream.Stream;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;

public class DefaultMethodHandlerFactory implements HandlerFactory {
    @Override
    public Optional<Handler> buildHandler(Class<?> sqlObjectType, Method method) {
        if (!method.isDefault()) {
            return Optional.empty();
        }

        Stream.of(method.getAnnotations())
                .map(Annotation::annotationType)
                .filter(type -> type.isAnnotationPresent(SqlStatementCustomizingAnnotation.class))
                .findFirst()
                .ifPresent(type -> {
                    throw new IllegalStateException(String.format(
                            "Default method %s.%s has @%s annotation. Statement customizing annotations don't " +
                                    "work on default methods.",
                            sqlObjectType.getSimpleName(),
                            method.getName(),
                            type.getSimpleName()));
                });

        for (Parameter parameter : method.getParameters()) {
            Stream.of(parameter.getAnnotations())
                    .map(Annotation::annotationType)
                    .filter(type -> type.isAnnotationPresent(SqlStatementCustomizingAnnotation.class))
                    .findFirst()
                    .ifPresent(type -> {
                        throw new IllegalStateException(String.format(
                                "Default method %s.%s parameter %s has @%s annotation. Statement customizing " +
                                        "annotations don't work on default methods.",
                                sqlObjectType.getSimpleName(),
                                method.getName(),
                                parameter.getName(),
                                type.getSimpleName()));
                    });
        }

        return Optional.of(new DefaultMethodHandler(method));
    }
}

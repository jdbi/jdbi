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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jdbi.v3.core.internal.Throwables;

import static java.util.stream.Collectors.toList;

class SqlMethodHandlerFactory implements HandlerFactory {
    @Override
    public Optional<Handler> buildHandler(Class<?> sqlObjectType, Method method) {
        List<Class<?>> sqlMethodAnnotations = Stream.of(method.getAnnotations())
                .map(Annotation::annotationType)
                .filter(type -> type.isAnnotationPresent(SqlOperation.class))
                .collect(toList());

        if (sqlMethodAnnotations.isEmpty()) {
            return Optional.empty();
        }

        if (sqlMethodAnnotations.size() > 1) {
            throw new IllegalStateException(
                    String.format("Mutually exclusive annotations on method %s.%s: %s",
                            sqlObjectType.getName(),
                            method.getName(),
                            sqlMethodAnnotations));
        }

        if (method.isDefault() && !method.isSynthetic()) {
            throw new IllegalStateException(String.format(
                    "Default method %s.%s has @%s annotation. "
                            + "SQL object methods may be default, or have a SQL method annotation, but not both.",
                    sqlObjectType.getSimpleName(),
                    method.getName(),
                    sqlMethodAnnotations.get(0).getSimpleName()));
        }

        return Optional.of(sqlMethodAnnotations.stream()
                .map(type -> type.getAnnotation(SqlOperation.class))
                .map(a -> buildHandler(a.value(), sqlObjectType, method))
                .findFirst()
                .<IllegalStateException>orElseThrow(() -> new IllegalStateException(String.format(
                        "Method %s.%s must be default or be annotated with a SQL method annotation.",
                        sqlObjectType.getSimpleName(),
                        method.getName()))));
    }

    @SuppressWarnings("PMD.PreserveStackTrace")
    private Handler buildHandler(Class<? extends Handler> handlerType, Class<?> sqlObjectType, Method method) {
        try {
            return handlerType.getConstructor(Class.class, Method.class).newInstance(sqlObjectType, method);
        } catch (InvocationTargetException e) {
            throw Throwables.throwOnlyUnchecked(e.getCause());
        } catch (ReflectiveOperationException ignored) {
            // fall-through
        }

        try {
            return handlerType.getConstructor(Method.class).newInstance(method);
        } catch (InvocationTargetException e) {
            throw Throwables.throwOnlyUnchecked(e.getCause());
        } catch (ReflectiveOperationException ignored) {
            // fall-through
        }

        try {
            return handlerType.getConstructor().newInstance();
        } catch (InvocationTargetException e) {
            throw Throwables.throwOnlyUnchecked(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Handler class " + handlerType + " cannot be instantiated. "
                    + "Expected a constructor with parameters (Class, Method), (Method), or ().", e);
        }
    }
}

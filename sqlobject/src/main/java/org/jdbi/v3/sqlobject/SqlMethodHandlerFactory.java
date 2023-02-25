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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.extension.ExtensionHandler;
import org.jdbi.v3.core.extension.ExtensionHandler.ExtensionHandlerFactory;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.core.internal.exceptions.CheckedCallable;

import static java.lang.String.format;

class SqlMethodHandlerFactory implements ExtensionHandlerFactory {

    @Override
    public boolean accepts(Class<?> extensionType, Method method) {

        if (method.isBridge()) {
            return false;
        }

        return !SqlObjectAnnotationHelper.findSqlMethodAnnotations(method).isEmpty();
    }

    @Override
    public Optional<ExtensionHandler> buildExtensionHandler(Class<?> sqlObjectType, Method method) {

        List<Class<?>> sqlMethodAnnotations = SqlObjectAnnotationHelper.findSqlMethodAnnotations(method);

        if (sqlMethodAnnotations.size() > 1) {
            throw new IllegalStateException(
                    format("Mutually exclusive annotations on method %s.%s: %s",
                            sqlObjectType.getName(),
                            method.getName(),
                            sqlMethodAnnotations));
        }

        if (method.isDefault() && !method.isSynthetic()) {
            throw new IllegalStateException(format(
                    "Default method %s.%s has @%s annotation. "
                            + "SQL object methods may be default, or have a SQL method annotation, but not both.",
                    sqlObjectType.getSimpleName(),
                    method.getName(),
                    sqlMethodAnnotations.get(0).getSimpleName()));
        }

        return Optional.of(SqlObjectAnnotationHelper.findOldAnnotations(method)
                .map(type -> type.getAnnotation(SqlOperation.class))
                .map(a -> createHandler(a.value(), sqlObjectType, method))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(format(
                        "Method %s.%s must be default or be annotated with a SQL method annotation.",
                        sqlObjectType.getSimpleName(),
                        method.getName()))));
    }


    private Handler createHandler(Class<? extends Handler> handlerType, Class<?> sqlObjectType, Method method) {

        CheckedCallable[] callables = {
                () -> handlerType.getConstructor(Class.class, Method.class).newInstance(sqlObjectType, method),
                () -> handlerType.getConstructor(Method.class).newInstance(method),
                () -> handlerType.getConstructor().newInstance()
        };

        for (CheckedCallable<Handler> callable : callables) {
            Optional<Handler> handler = JdbiClassUtils.createInstanceIfPossible(callable);
            if (handler.isPresent()) {
                return handler.get();
            }
        }

        throw new IllegalStateException(format("Handler class %s cannot be instantiated. "
                + "Expected a constructor with parameters (Class, Method), (Method), or ().", handlerType));
    }
}

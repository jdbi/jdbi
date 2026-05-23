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
import org.jdbi.v3.core.extension.ExtensionHandlerFactory;
import org.jdbi.v3.core.internal.JdbiClassUtils;

import static java.lang.String.format;

class SqlMethodHandlerFactory implements ExtensionHandlerFactory {

    private static final Class<?>[] SQL_METHOD_HANDLER_TYPES = {Class.class, Method.class};

    @Override
    public boolean accepts(Class<?> extensionType, Method method) {
        return !method.isBridge() && !SqlObjectAnnotationHelper.findSqlMethodAnnotations(method).isEmpty();
    }

    @Override
    public Optional<ExtensionHandler> createExtensionHandler(Class<?> sqlObjectType, Method method) {

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
                .map(SqlOperation::value)
                .map(klass -> JdbiClassUtils.findConstructorAndCreateInstance(klass, SQL_METHOD_HANDLER_TYPES,
                        handle -> handle.invokeExact(sqlObjectType, method)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(format(
                        "Method %s.%s must be default or be annotated with a SQL method annotation.",
                        sqlObjectType.getSimpleName(),
                        method.getName()))));
    }
}

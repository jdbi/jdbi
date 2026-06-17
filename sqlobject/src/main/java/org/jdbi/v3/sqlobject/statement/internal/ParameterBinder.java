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
package org.jdbi.v3.sqlobject.statement.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;

import static org.jdbi.v3.core.internal.JdbiClassUtils.checkedCreateInstance;

/**
 * Binds a SQL Object method's arguments to an arbitrary {@link SqlStatement}, reusing the
 * parameter binding annotations (such as {@code @Bind} and {@code @BindBean}) and the default
 * by-name binding. Used to bind method arguments to the statements created for {@code @SqlPreflight}.
 *
 * <p>Unlike the full statement handler, this only applies <em>parameter</em> customizers; statement
 * and method customizers (for example {@code @FetchSize}, which only applies to queries) are not
 * applied. {@link Consumer} and {@link Function} parameters are skipped, as they are not bindable
 * argument values.
 *
 * <p>This is an immutable, single-threaded helper.
 */
final class ParameterBinder {

    private final List<Binder> binders;

    ParameterBinder(Class<?> sqlObjectType, Method method) {
        final List<Binder> result = new ArrayList<>();
        final Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            final int index = i;
            final Parameter parameter = parameters[i];
            final Type parameterType = GenericTypes.resolveType(parameter.getParameterizedType(), sqlObjectType);

            final List<SqlStatementParameterCustomizer> annotated = annotationsFor(parameter)
                    .map(a -> instantiateFactory(a).createForParameter(a, sqlObjectType, method, parameter, index, parameterType))
                    .toList();

            if (annotated.isEmpty()) {
                if (parameter.getType() == Consumer.class || parameter.getType() == Function.class) {
                    // not a bindable argument value
                    continue;
                }
                result.add((stmt, args, config) -> config.get(SqlObjects.class)
                        .getDefaultParameterCustomizerFactory()
                        .createForParameter(sqlObjectType, method, parameter, index, parameterType)
                        .apply(stmt, args[index]));
            } else {
                result.add((stmt, args, config) -> {
                    for (SqlStatementParameterCustomizer customizer : annotated) {
                        customizer.apply(stmt, args[index]);
                    }
                });
            }
        }

        this.binders = List.copyOf(result);
    }

    void apply(SqlStatement<?> stmt, Object[] args) throws SQLException {
        final ConfigRegistry config = stmt.getConfig();
        for (Binder binder : binders) {
            binder.bind(stmt, args, config);
        }
    }

    private static Stream<Annotation> annotationsFor(Parameter parameter) {
        return Stream.of(parameter.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(SqlStatementCustomizingAnnotation.class));
    }

    private static SqlStatementCustomizerFactory instantiateFactory(Annotation annotation) {
        SqlStatementCustomizingAnnotation customizingAnnotation = annotation.annotationType()
                .getAnnotation(SqlStatementCustomizingAnnotation.class);
        return checkedCreateInstance(customizingAnnotation.value());
    }

    @FunctionalInterface
    private interface Binder {
        void bind(SqlStatement<?> stmt, Object[] args, ConfigRegistry config) throws SQLException;
    }
}

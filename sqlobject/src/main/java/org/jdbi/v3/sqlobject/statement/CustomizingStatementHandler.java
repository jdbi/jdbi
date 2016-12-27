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
package org.jdbi.v3.sqlobject.statement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.Handler;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;

abstract class CustomizingStatementHandler<StatementType extends SqlStatement<StatementType>> implements Handler
{
    private final List<SqlStatementCustomizer>                typeBasedCustomizers;
    private final List<SqlStatementCustomizer>                methodBasedCustomizers;
    private final List<ParamCustomizer> paramBasedCustomizers = new ArrayList<>();
    private final Class<?> sqlObjectType;
    private final Method method;

    CustomizingStatementHandler(ConfigRegistry registry, Class<?> sqlObjectType, Method method)
    {
        this.sqlObjectType = sqlObjectType;
        this.method = method;

        typeBasedCustomizers = Stream.of(sqlObjectType.getAnnotations())
            .flatMap(a -> instantiateFactory(a).map(f -> f.createForType(registry, a, sqlObjectType)))
            .collect(Collectors.toList());

        methodBasedCustomizers = Stream.of(method.getAnnotations())
                .flatMap(a -> instantiateFactory(a).map(f -> f.createForMethod(registry, a, sqlObjectType, method)))
                .collect(Collectors.toList());

        final Parameter[] parameters = method.getParameters();
        final Annotation[][] paramAnnotations = method.getParameterAnnotations();
        for (int paramIndex = 0; paramIndex < parameters.length; paramIndex++) {
            final int capturedParamIndex = paramIndex;
            final Parameter param = parameters[paramIndex];
            final List<SqlStatementParameterCustomizer> customizers =
                    Stream.of(paramAnnotations[paramIndex])
                        .flatMap(a -> instantiateFactory(a).map(f ->
                                f.createForParameter(registry, a, sqlObjectType, method, param, capturedParamIndex)))
                        .collect(Collectors.toCollection(ArrayList::new));
            if (customizers.isEmpty()) {
                customizers.add(new Bind.Factory().createForParameter(
                        registry, null, sqlObjectType, method, param, paramIndex));
            }
            customizers.forEach(c -> paramBasedCustomizers.add((stmt, args) -> c.apply(stmt, args[capturedParamIndex])));
        }
    }

    @Override
    public Object invoke(Object target, Object[] args, HandleSupplier hs) throws Exception {
        final Handle h = hs.getHandle();
        final String locatedSql = locateSql(h);
        final StatementType stmt = createStatement(h, locatedSql);
        final SqlObjectStatementConfiguration cfg = stmt.getConfig(SqlObjectStatementConfiguration.class);
        configureReturner(stmt, cfg);
        applyCustomizers(stmt, args);
        return cfg.getReturner().get();
    }

    abstract void configureReturner(StatementType stmt, SqlObjectStatementConfiguration cfg);
    abstract StatementType createStatement(Handle handle, String locatedSql);

    String locateSql(final Handle h)
    {
        return h.getConfig(SqlObjects.class).getSqlLocator().locate(sqlObjectType, method);
    }

    void applyCustomizers(SqlStatement<?> stmt, Object[] args)
    {
        try {
            for (SqlStatementCustomizer c : typeBasedCustomizers) {
                c.apply(stmt);
            }
            for (SqlStatementCustomizer c : methodBasedCustomizers) {
                c.apply(stmt);
            }
            for (ParamCustomizer c : paramBasedCustomizers) {
                c.apply(stmt, args);
            }
        } catch (SQLException e) {
            throw new UnableToCreateStatementException("unable to apply customizer", e, stmt.getContext());
        }
    }

    Method getMethod()
    {
        return method;
    }

    Stream<SqlStatementCustomizerFactory> instantiateFactory(Annotation annotation)
    {
        final Class<? extends Annotation> annotationType = annotation.annotationType();
        if (!annotationType.isAnnotationPresent(SqlStatementCustomizingAnnotation.class)) {
            return Stream.empty();
        }
        final SqlStatementCustomizingAnnotation scf =
            annotationType.getAnnotation(SqlStatementCustomizingAnnotation.class);
        try {
            return Stream.of(scf.value().getConstructor().newInstance());
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException("unable to instantiate statement customizer factory", e);
        }
    }

    interface ParamCustomizer
    {
        void apply(SqlStatement<?> stmt, Object[] args) throws SQLException;
    }
}

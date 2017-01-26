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
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.Handler;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;

abstract class CustomizingStatementHandler<StatementType extends SqlStatement<StatementType>> implements Handler
{
    private final List<FactoryAnnotationPair>           typeBasedCustomizerFactories   = new ArrayList<>();
    private final List<FactoryAnnotationPair>           methodBasedCustomizerFactories = new ArrayList<>();
    private final List<FactoryAnnotationParameterIndex> paramBasedCustomizerFactories  = new ArrayList<>();
    private final Class<?> sqlObjectType;
    private final Method method;

    CustomizingStatementHandler(Class<?> sqlObjectType, Method method)
    {
        this.sqlObjectType = sqlObjectType;
        this.method = method;

        for (final Annotation annotation : sqlObjectType.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(SqlStatementCustomizingAnnotation.class)) {
                final SqlStatementCustomizingAnnotation a = annotation.annotationType()
                                                                      .getAnnotation(SqlStatementCustomizingAnnotation.class);
                final SqlStatementCustomizer f;
                try {
                    f = a.value().newInstance();
                }
                catch (Exception e) {
                    throw new IllegalStateException("unable to create sql statement customizer factory", e);
                }
                typeBasedCustomizerFactories.add(new FactoryAnnotationPair(f, annotation));
            }
        }


        for (final Annotation annotation : method.getAnnotations()) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(SqlStatementCustomizingAnnotation.class)) {
                final SqlStatementCustomizingAnnotation scf =
                    annotationType.getAnnotation(SqlStatementCustomizingAnnotation.class);
                final SqlStatementCustomizer f;
                try {
                    f = scf.value().newInstance();
                }
                catch (Exception e) {
                    throw new IllegalStateException("unable to instantiate statement customizer factory", e);
                }
                methodBasedCustomizerFactories.add(new FactoryAnnotationPair(f, annotation));
            }

        }

        final Annotation[][] paramAnnotations = method.getParameterAnnotations();
        final Parameter[] parameters = method.getParameters();
        for (int paramIndex = 0; paramIndex < paramAnnotations.length; paramIndex++) {
            boolean foundCustomizingAnnotations = false;
            for (final Annotation annotation : paramAnnotations[paramIndex]) {
                final Class<? extends Annotation> annotationType = annotation.annotationType();

                if (annotationType.isAnnotationPresent(SqlStatementCustomizingAnnotation.class)) {
                    SqlStatementCustomizingAnnotation sca = annotation.annotationType()
                                                                      .getAnnotation(SqlStatementCustomizingAnnotation.class);
                    final SqlStatementCustomizer f;
                    try {
                        f = sca.value().newInstance();
                    }
                    catch (Exception e) {
                        throw new IllegalStateException("unable to instantiate sql statement customizer factory", e);
                    }
                    paramBasedCustomizerFactories.add(new FactoryAnnotationParameterIndex(f, annotation, parameters[paramIndex], paramIndex));
                    foundCustomizingAnnotations = true;
                }
            }

            if (!foundCustomizingAnnotations) {
                // There are no customizing annotations on the parameter, so use default binder
                paramBasedCustomizerFactories.add(new FactoryAnnotationParameterIndex(new Bind.Factory(), null, parameters[paramIndex], paramIndex));
            }
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
            for (FactoryAnnotationPair pair : typeBasedCustomizerFactories) {
                pair.factory.customizeForType(stmt, pair.annotation, sqlObjectType);
            }

            for (FactoryAnnotationPair pair : methodBasedCustomizerFactories) {
                pair.factory.customizeForMethod(stmt, pair.annotation, sqlObjectType, method);
            }

            for (FactoryAnnotationParameterIndex param : paramBasedCustomizerFactories) {
                param.factory
                    .customizeForParameter(stmt, param.annotation, sqlObjectType, method, param.parameter, param.index, args[param.index]);
            }
        } catch (SQLException e) {
            throw new UnableToCreateStatementException("unable to apply customizer", e, stmt.getContext());
        }
    }

    private static class FactoryAnnotationPair
    {
        private final SqlStatementCustomizer factory;
        private final Annotation                    annotation;

        FactoryAnnotationPair(SqlStatementCustomizer factory, Annotation annotation)
        {
            this.factory = factory;
            this.annotation = annotation;
        }
    }

    private static class FactoryAnnotationParameterIndex
    {
        private final SqlStatementCustomizer factory;
        private final Annotation                    annotation;
        private final Parameter                     parameter;
        private final int                           index;

        FactoryAnnotationParameterIndex(SqlStatementCustomizer factory,
                                        Annotation annotation,
                                        Parameter parameter,
                                        int index)
        {
            this.factory = factory;
            this.annotation = annotation;
            this.parameter = parameter;
            this.index = index;
        }
    }

    Method getMethod()
    {
        return method;
    }

    static RowMapper<?> rowMapperFor(GetGeneratedKeys ggk, Type returnType)
    {
        if (GetGeneratedKeys.DefaultMapper.class.equals(ggk.value())) {
            return new GetGeneratedKeys.DefaultMapper(returnType, ggk.columnName());
        }
        else {
            try {
                return ggk.value().getConstructor().newInstance();
            }
            catch (Exception e) {
                throw new UnableToCreateStatementException("Unable to instantiate row mapper for statement", e, null);
            }
        }
    }
}

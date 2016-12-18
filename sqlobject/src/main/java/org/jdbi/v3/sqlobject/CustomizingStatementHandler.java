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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.core.statement.SqlStatement;

abstract class CustomizingStatementHandler implements Handler
{
    private final List<FactoryAnnotationPair>           typeBasedCustomizerFactories   = new ArrayList<>();
    private final List<FactoryAnnotationPair>           methodBasedCustomizerFactories = new ArrayList<>();
    private final List<FactoryAnnotationParameterIndex> paramBasedCustomizerFactories  = new ArrayList<>();
    private final Class<?> sqlObjectType;
    private final Method method;

    @SuppressWarnings("unchecked")
    CustomizingStatementHandler(Class<?> sqlObjectType, Method method)
    {
        this.sqlObjectType = sqlObjectType;
        this.method = method;

        for (final Annotation annotation : sqlObjectType.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(SqlStatementCustomizingAnnotation.class)) {
                final SqlStatementCustomizingAnnotation a = annotation.annotationType()
                                                                      .getAnnotation(SqlStatementCustomizingAnnotation.class);
                final SqlStatementCustomizerFactory f;
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
                final SqlStatementCustomizerFactory f;
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
                    final SqlStatementCustomizerFactory f;
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

    protected void applyCustomizers(SqlStatement<?> q, Object[] args)
    {
        for (FactoryAnnotationPair pair : typeBasedCustomizerFactories) {
            try {
                pair.factory.createForType(pair.annotation, sqlObjectType).apply(q);
            }
            catch (SQLException e) {
                throw new UnableToCreateStatementException("unable to apply customizer", e, q.getContext());
            }
        }

        for (FactoryAnnotationPair pair : methodBasedCustomizerFactories) {
            try {
                pair.factory.createForMethod(pair.annotation, sqlObjectType, method).apply(q);
            }
            catch (SQLException e) {
                throw new UnableToCreateStatementException("unable to apply customizer", e, q.getContext());
            }
        }

        if (args != null) {
            for (FactoryAnnotationParameterIndex param : paramBasedCustomizerFactories) {
                try {
                    param.factory
                        .createForParameter(param.annotation, sqlObjectType, method, param.parameter, param.index, args[param.index])
                        .apply(q);
                }
                catch (SQLException e) {
                    throw new UnableToCreateStatementException("unable to apply customizer", e, q.getContext());
                }
            }
        }
    }

    private static class FactoryAnnotationPair
    {
        private final SqlStatementCustomizerFactory factory;
        private final Annotation                    annotation;

        FactoryAnnotationPair(SqlStatementCustomizerFactory factory, Annotation annotation)
        {
            this.factory = factory;
            this.annotation = annotation;
        }
    }

    private static class FactoryAnnotationParameterIndex
    {
        private final SqlStatementCustomizerFactory factory;
        private final Annotation                    annotation;
        private final Parameter                     parameter;
        private final int                           index;

        FactoryAnnotationParameterIndex(SqlStatementCustomizerFactory factory,
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

    protected Method getMethod()
    {
        return method;
    }
}

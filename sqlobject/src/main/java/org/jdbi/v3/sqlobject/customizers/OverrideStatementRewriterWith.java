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
package org.jdbi.v3.sqlobject.customizers;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.rewriter.StatementRewriter;
import org.jdbi.v3.sqlobject.ConfigurerFactory;
import org.jdbi.v3.sqlobject.ConfiguringAnnotation;

/**
 * Use this to override the statement rewriter on a sql object, May be specified on either the interface
 * or method level.
 */
@Retention(RetentionPolicy.RUNTIME)
@ConfiguringAnnotation(OverrideStatementRewriterWith.Factory.class)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface OverrideStatementRewriterWith
{
    /**
     * Specify the StatementRewriter class to use.
     * @return the StatementRewriter class to use.
     */
    Class<? extends StatementRewriter> value();

    class Factory implements ConfigurerFactory
    {
        @Override
        public Consumer<ConfigRegistry> createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            OverrideStatementRewriterWith anno = (OverrideStatementRewriterWith) annotation;
            try {
                final StatementRewriter rw = instantiate(anno.value(), sqlObjectType, method);
                return config -> config.get(SqlStatements.class).setStatementRewriter(rw);
            }
            catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Consumer<ConfigRegistry> createForType(Annotation annotation, Class<?> sqlObjectType)
        {
            OverrideStatementRewriterWith anno = (OverrideStatementRewriterWith) annotation;
            try {
                final StatementRewriter rw = instantiate(anno.value(), sqlObjectType, null);
                return config -> config.get(SqlStatements.class).setStatementRewriter(rw);
            }
            catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private StatementRewriter instantiate(Class<? extends StatementRewriter> value,
                                              Class<?> sqlObjectType,
                                              Method m) throws Exception
        {
            try {
                Constructor<? extends StatementRewriter> no_arg = value.getConstructor();
                return no_arg.newInstance();
            }
            catch (NoSuchMethodException e) {
                try {
                    Constructor<? extends StatementRewriter> class_arg = value.getConstructor(Class.class);
                    return class_arg.newInstance(sqlObjectType);
                }
                catch (NoSuchMethodException e1) {
                    if (m != null) {
                        Constructor<? extends StatementRewriter> c_m_arg = value.getConstructor(Class.class,
                                                                                                Method.class);
                        return c_m_arg.newInstance(sqlObjectType, m);
                    }
                    throw new IllegalStateException("Unable to instantiate, no viable constructor " + value.getName());
                }
            }
        }
    }
}

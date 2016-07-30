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
package org.jdbi.v3.stringtemplate;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jdbi.v3.core.rewriter.ColonPrefixStatementRewriter;
import org.jdbi.v3.core.rewriter.StatementRewriter;
import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;

/**
 * Configures a SQL object class or method to rewrite SQL statements using StringTemplate. Method parameters annotated
 * with {@link org.jdbi.v3.sqlobject.customizers.Define @Define} are passed to the StringTemplate as template
 * attributes.
 */
@SqlStatementCustomizingAnnotation(UseStringTemplateStatementRewriter.Factory.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UseStringTemplateStatementRewriter {
    Class<? extends StatementRewriter> value() default ColonPrefixStatementRewriter.class;

    class Factory implements SqlStatementCustomizerFactory {
        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType) {
            return create((UseStringTemplateStatementRewriter) annotation);
        }

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
            return create((UseStringTemplateStatementRewriter) annotation);
        }

        private SqlStatementCustomizer create(UseStringTemplateStatementRewriter annotation) {
            StatementRewriter delegate = createDelegate(annotation.value());
            StatementRewriter rewriter = new StringTemplateStatementRewriter(delegate);
            return q -> q.setStatementRewriter(rewriter);
        }

        private StatementRewriter createDelegate(Class<? extends StatementRewriter> type) {
            try {
                return type.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalStateException("Error instantiating delegate statement rewriter: " + type, e);
            }
        }
    }
}

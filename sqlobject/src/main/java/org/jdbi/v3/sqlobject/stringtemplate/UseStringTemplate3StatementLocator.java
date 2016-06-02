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
package org.jdbi.v3.sqlobject.stringtemplate;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.concurrent.GuardedBy;

import org.antlr.stringtemplate.StringTemplateErrorListener;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.tweak.StatementLocator;

@SqlStatementCustomizingAnnotation(UseStringTemplate3StatementLocator.LocatorFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface UseStringTemplate3StatementLocator
{
    String DEFAULT_VALUE = " ~ ";

    String value() default DEFAULT_VALUE;

    Class<?> errorListener() default StringTemplateErrorListener.class;
    boolean cacheable() default true;

    class LocatorFactory implements SqlStatementCustomizerFactory
    {
        @GuardedBy("LocatorFactory.class")
        private static final Map<Class<?>, WeakHashMap<Annotation, SqlStatementCustomizer>> CUSTOMIZER_CACHE =
                new WeakHashMap<Class<?>, WeakHashMap<Annotation, SqlStatementCustomizer>>();

        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType)
        {
            final UseStringTemplate3StatementLocator a = (UseStringTemplate3StatementLocator) annotation;

            if (a.cacheable()) {
                synchronized (LocatorFactory.class) {
                    WeakHashMap<Annotation, SqlStatementCustomizer> classCache = CUSTOMIZER_CACHE.get(sqlObjectType);
                    if (classCache == null) {
                        CUSTOMIZER_CACHE.put(sqlObjectType, classCache = new WeakHashMap<Annotation, SqlStatementCustomizer>());
                    }
                    SqlStatementCustomizer cachedCustomizer = classCache.get(a);
                    if (cachedCustomizer != null) {
                        return cachedCustomizer;
                    }
                }
            }

            final StringTemplate3StatementLocator.Builder builder;

            if (DEFAULT_VALUE.equals(a.value())) {
                builder = StringTemplate3StatementLocator.builder(sqlObjectType);
            }
            else {
                builder = StringTemplate3StatementLocator.builder(a.value());
            }

            StringTemplateErrorListener errorListener = StringTemplateGroup.DEFAULT_ERROR_LISTENER;
            if (!StringTemplateErrorListener.class.equals(a.errorListener())) {
              try {
                errorListener = (StringTemplateErrorListener) a.errorListener().newInstance();
              }
              catch(Exception e) {
                throw new IllegalStateException("Error initializing StringTemplateErrorListener", e);
              }
            }

            final StatementLocator l = builder.allowImplicitTemplateGroup().treatLiteralsAsTemplates().shouldCache().withErrorListener(errorListener).build();

            final SqlStatementCustomizer result = q -> q.setStatementLocator(l);

            if (a.cacheable()) {
                synchronized (LocatorFactory.class) {
                    CUSTOMIZER_CACHE.get(sqlObjectType).put(a, result);
                }
            }

            return result;
        }
    }
}

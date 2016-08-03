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

import static org.jdbi.v3.stringtemplate.StringTemplateSqlLocator.findStringTemplateGroup;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.jdbi.v3.core.rewriter.ColonPrefixStatementRewriter;
import org.jdbi.v3.core.rewriter.StatementRewriter;
import org.jdbi.v3.sqlobject.SqlAnnotations;
import org.jdbi.v3.sqlobject.SqlObjectConfig;
import org.jdbi.v3.sqlobject.SqlObjectConfigurerFactory;
import org.jdbi.v3.sqlobject.SqlObjectConfiguringAnnotation;
import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.locator.SqlLocator;

/**
 * Configures SQL Object to locate SQL using the {@link StringTemplateSqlLocator#findStringTemplateSql(Class, String)}
 * method. If the SQL annotation (e.g. <code>@SqlQuery</code>) defines a value (e.g. <code>@SqlQuery("hello")</code>),
 * that value (<code>"hello"</code>) will be used for the <code>name</code> parameter; if undefined, the name of the SQL
 * object method will be used:
 *
 * <pre>
 *     &#064;UseStringTemplateSqlLocator
 *     interface Viccini {
 *         &#064;SqlUpdate
 *         void doTheThing(long id);     // =&gt; StringTemplateSqlLocator.findStringTemplateSql(Viccini.class, "doTheThing")
 *
 *         &#064;SqlUpdate("thatOtherThing")
 *         void doTheThing(String name); // =&gt; StringTemplateSqlLocator.findStringTemplateSql(Viccini.class, "thatOtherThing")
 *     }
 * </pre>
 */
@SqlObjectConfiguringAnnotation(UseStringTemplateSqlLocator.LocatorFactory.class)
@SqlStatementCustomizingAnnotation(UseStringTemplateSqlLocator.RewriterFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface UseStringTemplateSqlLocator {
    Class<? extends StatementRewriter> value() default ColonPrefixStatementRewriter.class;

    class LocatorFactory implements SqlObjectConfigurerFactory {
        private static final Map<String, Class<?>> TYPE_CACHE = new ConcurrentHashMap<>();

        private static final SqlLocator SQL_LOCATOR = (sqlObjectType, method) -> {
            String name = SqlAnnotations.getAnnotationValue(method).orElseGet(method::getName);
            StringTemplateGroup group = findStringTemplateGroup(sqlObjectType);
            if (!group.isDefined(name)) {
                throw new IllegalStateException("No StringTemplate group " + name + " for class " + sqlObjectType);
            }

            String typeName = sqlObjectType.getName();
            TYPE_CACHE.putIfAbsent(typeName, sqlObjectType);

            // Hash character # should never appear in class names.. right?
            return typeName + "#" + name;
        };

        private static final Consumer<SqlObjectConfig> CONFIGURER = config -> config.setSqlLocator(SQL_LOCATOR);

        @Override
        public Consumer<SqlObjectConfig> createForType(Annotation annotation, Class<?> sqlObjectType) {
            return CONFIGURER;
        }

        @Override
        public Consumer<SqlObjectConfig> createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
            return CONFIGURER;
        }
    }

    class RewriterFactory implements SqlStatementCustomizerFactory {
        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType) {
            return create((UseStringTemplateSqlLocator) annotation);
        }

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
            return create((UseStringTemplateSqlLocator) annotation);
        }

        private SqlStatementCustomizer create(UseStringTemplateSqlLocator annotation) {
            StatementRewriter delegate = createDelegate(annotation.value());
            StatementRewriter locatingRewriter = (sql, params, ctx) -> {
                int delimiterIndex = sql.indexOf('#');
                String typeName = sql.substring(0, delimiterIndex);
                String templateName = sql.substring(delimiterIndex + 1);

                Class<?> type = LocatorFactory.TYPE_CACHE.get(typeName);
                StringTemplateGroup group = findStringTemplateGroup(type);
                StringTemplate template = group.getInstanceOf(templateName, ctx.getAttributes());
                String rewritten = template.toString();

                return delegate.rewrite(rewritten, params, ctx);
            };
            return q -> q.setStatementRewriter(locatingRewriter);
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

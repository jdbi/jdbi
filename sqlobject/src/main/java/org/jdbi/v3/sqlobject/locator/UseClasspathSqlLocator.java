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
package org.jdbi.v3.sqlobject.locator;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.locator.ClasspathSqlLocator;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.internal.SqlAnnotations;

/**
 * Configures SQL Object to locate SQL using the {@link ClasspathSqlLocator#findSqlOnClasspath(Class, String)} method.
 * If the SQL annotation (e.g. <code>@SqlQuery</code>) defines a value (e.g. <code>@SqlQuery("hello")</code>), that
 * value (<code>"hello"</code>) will be used for the <code>name</code> parameter; if undefined, the name of the SQL
 * object method will be used:
 *
 * <pre>
 *     &#064;UseClasspathSqlLocator
 *     interface Viccini {
 *         &#064;SqlUpdate
 *         void doTheThing(long id);     // =&gt; ClasspathSqlLocator.findSqlOnClasspath(Viccini.class, "doTheThing")
 *
 *         &#064;SqlUpdate("thatOtherThing")
 *         void doTheThing(String name); // =&gt; ClasspathSqlLocator.findSqlOnClasspath(Viccini.class, "thatOtherThing")
 *     }
 * </pre>
 */
@SqlStatementCustomizingAnnotation(UseClasspathSqlLocator.Factory.class)
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface UseClasspathSqlLocator {
    class Factory implements SqlStatementCustomizerFactory {
        private static final SqlLocator SQL_LOCATOR = (sqlObjectType, method) -> {
            String name = SqlAnnotations.getAnnotationValue(method).orElseGet(method::getName);
            return ClasspathSqlLocator.findSqlOnClasspath(sqlObjectType, name);
        };

        @Override
        public SqlStatementCustomizer createForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType) {
            registry.get(SqlObjects.class).setSqlLocator(SQL_LOCATOR);
            return NONE;
        }
    }
}

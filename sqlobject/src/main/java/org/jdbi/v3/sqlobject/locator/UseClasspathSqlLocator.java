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

import java.lang.annotation.Annotation;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.jdbi.v3.locator.ClasspathSqlLocator;
import org.jdbi.v3.sqlobject.SqlAnnotations;
import org.jdbi.v3.sqlobject.SqlObjectConfigurer;
import org.jdbi.v3.sqlobject.SqlObjectConfigurerFactory;
import org.jdbi.v3.sqlobject.SqlObjectConfiguringAnnotation;

/**
 * Configures SQL Object to locate SQL using the {@link ClasspathSqlLocator#findSqlOnClasspath(Class, String)} method.
 * If the SQL annotation (e.g. <code>@SqlQuery</code>) defines a value (e.g. <code>@SqlQuery("hello")</code>), that
 * value (<code>"hello"</code>) will be used for the <code>name</code> parameter; if undefined, the name of the SQL
 * object method will be used:
 *
 * <pre>
 *     &amp;UseClasspathSqlLocator
 *     interface Viccini {
 *         &amp;SqlUpdate
 *         void doTheThing(long id);     // =&gt; ClasspathSqlLocator.findSqlOnClasspath(Viccini.class, "doTheThing")
 *
 *         &amp;SqlUpdate("thatOtherThing")
 *         void doTheThing(String name); // =&gt; ClasspathSqlLocator.findSqlOnClasspath(Viccini.class, "thatOtherThing")
 *     }
 * </pre>
 */
@SqlObjectConfiguringAnnotation(UseClasspathSqlLocator.Factory.class)
@Target({TYPE, METHOD})
public @interface UseClasspathSqlLocator {
    class Factory implements SqlObjectConfigurerFactory {
        private static final SqlLocator SQL_LOCATOR = (sqlObjectType, method) -> {
            String name = SqlAnnotations.getAnnotationValue(method).orElseGet(method::getName);
            return ClasspathSqlLocator.findSqlOnClasspath(sqlObjectType, name);
        };

        private static SqlObjectConfigurer CONFIGURER = config -> config.setSqlLocator(SQL_LOCATOR);

        @Override
        public SqlObjectConfigurer createForType(Annotation annotation, Class<?> sqlObjectType) {
            return CONFIGURER;
        }

        @Override
        public SqlObjectConfigurer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
            return CONFIGURER;
        }
    }
}

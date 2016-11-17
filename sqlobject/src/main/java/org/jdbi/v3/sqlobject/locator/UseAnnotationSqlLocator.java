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
import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.jdbi.v3.core.ConfigRegistry;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.ConfigurerFactory;
import org.jdbi.v3.sqlobject.ConfiguringAnnotation;

/**
 * Configures SQL Object to use AnnotationSqlLocator (the default SqlLocator).
 */
@ConfiguringAnnotation(UseAnnotationSqlLocator.Factory.class)
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface UseAnnotationSqlLocator {
    class Factory implements ConfigurerFactory {
        private static Consumer<ConfigRegistry> CONFIGURER = config ->
                config.get(SqlObjects.class).setSqlLocator(new AnnotationSqlLocator());

        @Override
        public Consumer<ConfigRegistry> createForType(Annotation annotation, Class<?> sqlObjectType) {
            return CONFIGURER;
        }

        @Override
        public Consumer<ConfigRegistry> createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
            return CONFIGURER;
        }
    }
}

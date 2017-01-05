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
package org.jdbi.v3.sqlobject.config;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.mapper.RowMapperFactory;

/**
 * Used to register a row mapper factory with either a sql object type or for a specific method.
 */
@ConfiguringAnnotation(RegisterRowMapperFactory.Impl.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RegisterRowMapperFactory
{
    Class<? extends RowMapperFactory>[] value();

    class Impl implements Configurer
    {

        @Override
        public void configureForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            configureForType(registry, annotation, sqlObjectType);
        }

        @Override
        public void configureForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType)
        {
            RegisterRowMapperFactory registerRowMapperFactory = (RegisterRowMapperFactory) annotation;
            RowMappers mappers = registry.get(RowMappers.class);
            try {
                Class<? extends RowMapperFactory>[] factoryTypes = registerRowMapperFactory.value();
                for (int i = 0; i < factoryTypes.length; i++) {
                    mappers.register(factoryTypes[i].newInstance());
                }
            }
            catch (Exception e) {
                throw new IllegalStateException("unable to create a specified row mapper factory", e);
            }
        }
    }
}

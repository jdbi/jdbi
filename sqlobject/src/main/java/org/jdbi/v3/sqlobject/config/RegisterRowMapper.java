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
import org.jdbi.v3.core.mapper.RowMapper;

/**
 * Used to register a row mapper with either a sql object type or for a specific method.
 */
@ConfiguringAnnotation(RegisterRowMapper.Impl.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RegisterRowMapper
{
    /**
     * The row mapper classes to register
     * @return one or more row mapper classes
     */
    Class<? extends RowMapper<?>>[] value();

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
            RegisterRowMapper registerRowMapper = (RegisterRowMapper) annotation;
            RowMappers mappers = registry.get(RowMappers.class);
            try {
                Class<? extends RowMapper<?>>[] rowMapperTypes = registerRowMapper.value();
                for (int i = 0; i < rowMapperTypes.length; i++) {
                    mappers.register(rowMapperTypes[i].newInstance());
                }
            }
            catch (Exception e) {
                throw new IllegalStateException("unable to create a specified row mapper", e);
            }
        }
    }
}

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
package org.jdbi.v3.sqlobject.config.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.sqlobject.config.Configurer;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;

public class RegisterBeanMapperImpl implements Configurer
{
    @Override
    public void configureForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType) {
        RegisterBeanMapper registerBeanMapper = (RegisterBeanMapper) annotation;
        Class<?>[] beanClasses = registerBeanMapper.value();
        String[] prefixes = registerBeanMapper.prefix();
        RowMappers mappers = registry.get(RowMappers.class);
        if (prefixes.length == 0) {
            for (Class<?> beanClass : beanClasses) {
                mappers.register(BeanMapper.factory(beanClass));
            }
        }
        else if (prefixes.length == beanClasses.length) {
            for (int i = 0; i < beanClasses.length; i++) {
                mappers.register(BeanMapper.factory(beanClasses[i], prefixes[i]));
            }
        }
        else {
            throw new IllegalStateException("RegisterBeanMapper.prefix() must have the same number of elements as value()");
        }
    }

    @Override
    public void configureForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method)
    {
        configureForType(registry, annotation, sqlObjectType);
    }
}

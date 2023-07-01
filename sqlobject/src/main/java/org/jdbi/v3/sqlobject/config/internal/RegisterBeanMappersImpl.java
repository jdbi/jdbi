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
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.SimpleExtensionConfigurer;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterBeanMappers;

public class RegisterBeanMappersImpl extends SimpleExtensionConfigurer {

    private final List<RowMapperFactory> beanMappers;

    public RegisterBeanMappersImpl(Annotation annotation) {
        RegisterBeanMappers registerBeanMappers = (RegisterBeanMappers) annotation;
        this.beanMappers = new ArrayList<>(registerBeanMappers.value().length);

        for (RegisterBeanMapper registerBeanMapper : registerBeanMappers.value()) {
            Class<?> beanClass = registerBeanMapper.value();
            String prefix = registerBeanMapper.prefix();

            if (prefix.isEmpty()) {
                this.beanMappers.add(BeanMapper.factory(beanClass));
            } else {
                this.beanMappers.add(BeanMapper.factory(beanClass, prefix));
            }
        }
    }

    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
        RowMappers rowMappers = config.get(RowMappers.class);
        beanMappers.forEach(rowMappers::register);
    }
}

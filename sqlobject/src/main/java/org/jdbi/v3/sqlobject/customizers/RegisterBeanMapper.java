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
package org.jdbi.v3.sqlobject.customizers;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jdbi.v3.core.ConfigRegistry;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.sqlobject.ConfigurerFactory;
import org.jdbi.v3.sqlobject.ConfiguringAnnotation;

@Retention(RetentionPolicy.RUNTIME)
@ConfiguringAnnotation(RegisterBeanMapper.Factory.class)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RegisterBeanMapper
{
    /**
     * The bean classes to map with BeanMapper.
     * @return one or more bean classes.
     */
    Class<?>[] value();

    /**
     * Column name prefix for each bean type. If omitted, defaults to no prefix. If specified, must have the same
     * number of elements as {@link #value()}. Each <code>prefix</code> element is applied to the <code>value</code>
     * element at the same index.
     *
     * @return Column name prefixes corresponding pairwise to the elements in {@link #value()}.
     */
    String[] prefix() default {};

    class Factory implements ConfigurerFactory
    {
        @Override
        public Consumer<ConfigRegistry> createForType(Annotation annotation, Class<?> sqlObjectType) {
            return create((RegisterBeanMapper) annotation);
        }

        @Override
        public Consumer<ConfigRegistry> createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            return create((RegisterBeanMapper) annotation);
        }

        private Consumer<ConfigRegistry> create(RegisterBeanMapper annotation) {
            Class<?>[] beanClasses = annotation.value();
            String[] prefixes = annotation.prefix();
            List<RowMapperFactory> mappers = new ArrayList<>(beanClasses.length);
            if (prefixes.length == 0) {
                for (Class<?> beanClass : beanClasses) {
                    mappers.add(BeanMapper.of(beanClass));
                }
            }
            else if (prefixes.length == beanClasses.length) {
                for (int i = 0; i < beanClasses.length; i++) {
                    mappers.add(BeanMapper.of(beanClasses[i], prefixes[i]));
                }
            }
            else {
                throw new IllegalStateException("RegisterBeanMapper.prefix() must have the same number of elements as value()");
            }

            return config -> mappers.forEach(config.get(RowMappers.class)::register);
        }
    }
}

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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.sqlobject.config.internal.RegisterBeanMapperImpl;

@Retention(RetentionPolicy.RUNTIME)
@ConfiguringAnnotation(RegisterBeanMapperImpl.class)
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
}

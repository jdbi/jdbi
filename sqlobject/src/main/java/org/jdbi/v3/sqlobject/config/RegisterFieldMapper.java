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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.sqlobject.config.internal.RegisterFieldMapperImpl;

/**
 * Register types to reflectively assign fields with {@link FieldMapper}.
 */
@Retention(RetentionPolicy.RUNTIME)
@ConfiguringAnnotation(RegisterFieldMapperImpl.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(RegisterFieldMappers.class)
public @interface RegisterFieldMapper {
    /**
     * The type to map with FieldMapper.
     * @return the mapped type.
     */
    Class<?> value();

    /**
     * Column name prefix for the mapped type. If omitted, defaults to no prefix.
     *
     * @return Column name prefix for the mapped type.
     */
    String prefix() default "";
}

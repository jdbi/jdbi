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

import org.jdbi.v3.sqlobject.config.internal.RegisterImmutableMapperImpl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@ConfiguringAnnotation(RegisterImmutableMapperImpl.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(RegisterImmutableMappers.class)
public @interface RegisterImmutableMapper {
    /**
     * The mapped immutable class.
     * @return the mapped immutable class.
     */
    Class<?> value();

    /**
     * Column name prefix for the immutable type. If omitted, defaults to no prefix.
     *
     * @return Column name prefix for the immutable type.
     */
    String prefix() default "";
}

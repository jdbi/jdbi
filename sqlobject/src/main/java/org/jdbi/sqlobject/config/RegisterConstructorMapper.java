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
package org.jdbi.sqlobject.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.core.extension.annotation.UseExtensionConfigurer;
import org.jdbi.sqlobject.config.internal.RegisterConstructorMapperImpl;

/**
 * Registers a constructor mapper factory for the given type(s).
 */
@UseExtensionConfigurer(RegisterConstructorMapperImpl.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(RegisterConstructorMappers.class)
public @interface RegisterConstructorMapper {
    /**
     * The mapped type.
     * @return the mapped type.
     */
    Class<?> value();

    /**
     * Column name prefix for the mapped type. If omitted, defaults to no prefix.
     *
     * @return Column name prefix for the mapped type.
     */
    String prefix() default "";

    /**
     * If specified, find a static factory method in the given class to instantiate the mapped type.
     *
     * @return The type to inspect for a static factory method
     */
    Class<?> usingStaticMethodIn() default void.class;
}

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
package org.jdbi.v3.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configure reflective bean and pojo property attributes.
 * Most reflective mappers, including field, method, and property mappers, try to respect this.
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JdbiProperty {

    /**
     * Returns true if the property is mapped in a result. The property will be read from the database into a result.
     *
     * @return true if the property is mappable
     */
    boolean map() default true;

    /**
     * Returns true if the property is bound as an argument. Property will be bound as an argument.
     *
     * @return true if the property is bindable
     */
    boolean bind() default true;
}

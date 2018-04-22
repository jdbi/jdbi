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
package org.jdbi.v3.sqlobject.customizer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.sqlobject.customizer.internal.BindFieldsFactory;

/**
 * Binds the properties and public fields of an object to a SQL statement.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@SqlStatementCustomizingAnnotation(BindFieldsFactory.class)
public @interface BindFields {
    /**
     * Prefix to apply to each bean field name. If specified, fields will be bound as
     * {@code prefix.propertyName}.
     *
     * @return the prefix
     */
    String value() default "";
}

